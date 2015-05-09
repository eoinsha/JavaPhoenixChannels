package org.phoenixframework.channels;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.*;

public class Socket {
    private static final Logger LOG = Logger.getLogger(Socket.class.getName());

    public static final int MAX_RESEND_MESSAGES = 50;

    public static final int RECONNECT_INTERVAL_MS = 5000;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WebSocketContainer wsContainer = ContainerProvider.getWebSocketContainer();
    private Session wsSession = null;
    
    private String endpointUri = null;
    private List<Channel> channels = new ArrayList<Channel>();

    private Timer reconnectTimer = null;
    private TimerTask reconnectTimerTask = null;

    private LinkedBlockingQueue<Envelope> resendQueue = new LinkedBlockingQueue<Envelope>(MAX_RESEND_MESSAGES);

    private Set<ISocketOpenCallback> socketOpenCallbacks = Collections.newSetFromMap(new WeakHashMap<ISocketOpenCallback, Boolean>());
    private Set<ISocketCloseCallback> socketCloseCallbacks = Collections.newSetFromMap(new WeakHashMap<ISocketCloseCallback, Boolean>());
    private Set<IErrorCallback> errorCallbacks = Collections.newSetFromMap(new WeakHashMap<IErrorCallback, Boolean>());
    private Set<IMessageCallback> messageCallbacks = Collections.newSetFromMap(new WeakHashMap<IMessageCallback, Boolean>());

    private int refNo = 1;

    /**
     * Annotated WS Endpoint. Private member to prevent confusion with "onConn*" registration methods.
     */
    private PhoenixWSEndpoint wsEndpoint = new PhoenixWSEndpoint();

    @ClientEndpoint
    public class PhoenixWSEndpoint{
        private PhoenixWSEndpoint(){}

        @OnOpen
        public void onConnOpen(final Session session) {
            LOG.log(Level.FINE, "WebSocket onOpen: {0}", session);
            Socket.this.wsSession = session;
            if(Socket.this.reconnectTimerTask != null) {
                Socket.this.reconnectTimerTask.cancel();
            }

            // TODO - Heartbeat

            for(final ISocketOpenCallback callback : socketOpenCallbacks) {
                callback.onOpen();
            }
        }

        @OnClose
        public void onConnClose(final Session session, final CloseReason reason) {
            LOG.log(Level.FINE, "WebSocket onClose {0}, {1}", new Object[]{session, reason});
            Socket.this.wsSession = null;
            if(Socket.this.reconnectTimerTask != null) {
                Socket.this.reconnectTimerTask.cancel();
            }

            // TODO - Clear heartbeat timer

            Socket.this.reconnectTimerTask = new TimerTask() {
                @Override
                public void run() {
                    LOG.log(Level.FINE, "reconnectTimerTask run");
                    try {
                        Socket.this.connect();
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "Failed to reconnect to " + Socket.this.wsEndpoint, e);
                    }
                }
            };
            reconnectTimer.schedule(Socket.this.reconnectTimerTask, RECONNECT_INTERVAL_MS, RECONNECT_INTERVAL_MS);

            for(final ISocketCloseCallback callback : socketCloseCallbacks) {
                callback.onClose();
            }
        }

        @OnMessage
        public void onConnTextMessage(final String payloadText) {
            LOG.log(Level.FINE, "Envelope received: {0}", payloadText);
            try {
                final Envelope envelope = objectMapper.readValue(payloadText, Envelope.class);
                for(final Channel channel : channels) {
                    if(channel.isMember(envelope.getTopic())) {
                        channel.trigger(envelope.getEvent(), envelope);
                    }
                }

                for(final IMessageCallback callback : messageCallbacks) {
                    callback.onMessage(envelope);
                }
            } catch (IOException e) {
                // TODO: log, signal
                e.printStackTrace();
            }
        }

        @OnError
        public void onConnError(final Throwable error) {
            LOG.log(Level.WARNING, "WebSocket connection error", error);
            for(final IErrorCallback callback : errorCallbacks) {
                triggerChannelError();
                callback.onError(error.toString());
            }
        }
    }

    public Socket(final String endpointUri) throws IOException, DeploymentException {
        LOG.log(Level.FINE, "PhoenixSocket({0})", endpointUri);
        this.endpointUri = endpointUri;
        this.reconnectTimer = new Timer("Reconnect Timer for " + endpointUri);
    }

    public void disconnect() throws IOException {
        LOG.log(Level.FINE, "disconnect");
        if(wsSession != null && wsSession.isOpen()) {
            wsSession.close();
        }
    }

    public void connect() throws IOException, DeploymentException {
        LOG.log(Level.FINE, "connect");
        disconnect();
        wsContainer.connectToServer(wsEndpoint, URI.create(this.endpointUri));
    }

    /**
     * @return true if the socket connection is connected
     */
    public boolean isConnected() {
        return wsSession != null && wsSession.isOpen();
    }


    /**
     * Retrieve a channel instance for the specified topic
     *
     * @param topic
     * @param payload
     *
     * @throws IOException
     */
    public Channel chan(final String topic, final Payload payload) throws IOException {
        LOG.log(Level.FINE, "chan: {0}, {1}", new Object[]{topic, payload});
        final Channel channel = new Channel(topic, payload, Socket.this);
        synchronized(channels) {
            channels.add(channel);
        }
        return channel;
    }

    /**
     * Removes the specified channel if it is known to the socket
     *
     * @param channel
     */
    public void remove(final Channel channel) {
        synchronized (channels) {
            for(Iterator chanIter = channels.iterator(); chanIter.hasNext();) {
                if(chanIter.next() == channel) {
                    chanIter.remove();
                    break;
                }
            }
        }
    }

    /**
     * TODO - Propagate exception differently
     *
     * @param envelope
     * @throws IOException
     */
    public Socket push(final Envelope envelope) throws IOException {
        LOG.log(Level.FINE, "Pushing envelope: {0}", envelope);
        if(this.isConnected()) {
            final ObjectNode node = objectMapper.createObjectNode();
            node.put("topic", envelope.getTopic());
            node.put("event", envelope.getEvent());
            node.put("ref", envelope.getRef());
            node.putPOJO("payload", envelope.getPayload() == null ? new Payload() : envelope.getPayload());
            final String json = objectMapper.writeValueAsString(node);
            LOG.log(Level.FINE, "Sending JSON: {0}", json);
            wsSession.getAsyncRemote().sendText(json);
        }
        // TODO - Queue data if not connected

        return this;
    }

    /**
     * Register a callback for SocketEvent.OPEN events
     *
     * @param callback
     */
    public Socket onOpen(final ISocketOpenCallback callback ) {
        this.socketOpenCallbacks.add(callback);
        return this;
    }

    /**
     * Register a callback for SocketEvent.ERROR events
     *
     * @param callback
     */
    public Socket onClose(final ISocketCloseCallback callback ) {
        this.socketCloseCallbacks.add(callback);
        return this;
    }

    /**
     * Register a callback for SocketEvent.ERROR events
     *
     * @param callback
     */
    public Socket onError(final IErrorCallback callback ) {
        this.errorCallbacks.add(callback);
        return this;
    }

    /**
     * Register a callback for SocketEvent.MESSAGE events
     *
     * @param callback
     */
    public Socket onMessage(final IMessageCallback callback ) {
        this.messageCallbacks.add(callback);
        return this;
    }

    @Override
    public String toString() {
        return "PhoenixSocket{" +
                "endpointUri='" + endpointUri + '\'' +
                ", channels=" + channels +
                ", refNo=" + refNo +
                ", wsSession=" + wsSession +
                '}';
    }

    synchronized String makeRef() {
        int val = refNo++;
        if(refNo == Integer.MAX_VALUE) {
            refNo = 0;
        }
        return Integer.toString(val);
    }

    private void triggerChannelError() {
        for(final Channel channel : channels) {
            channel.trigger(ChannelEvent.ERROR.getPhxEvent(), null);
        }
    }

    static String replyEventName(final String ref) {
        return "chan_reply_" + ref;
    }
}