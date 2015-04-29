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

@ClientEndpoint
public class Socket {
    private static final Logger LOG = Logger.getLogger(Socket.class.getName());

    public static final int MAX_RESEND_MESSAGES = 50;

    private static final int RECONNECT_INTERVAL_MS = 5000;

    @Override
    public String toString() {
        return "PhoenixSocket{" +
                "endpointUri='" + endpointUri + '\'' +
                ", channels=" + channels +
                ", refNo=" + refNo +
                ", wsSession=" + wsSession +
                '}';
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WebSocketContainer wsContainer = ContainerProvider.getWebSocketContainer();
    private Session wsSession = null;
    
    private String endpointUri = null;
    private List<Channel> channels = new ArrayList<Channel>();

    private Timer reconnectTimer = null;
    private TimerTask reconnectTimerTask = null;

    private LinkedBlockingQueue<Envelope> resendQueue = new LinkedBlockingQueue<Envelope>(MAX_RESEND_MESSAGES);

    private Map<SocketEvent, List<SocketCallback>> socketEventCallbacks = new HashMap<SocketEvent, List<SocketCallback>>();
    {
        for(SocketEvent ev : SocketEvent.values()) {
            socketEventCallbacks.put(ev, new ArrayList<SocketCallback>());
        }
    }

    private int refNo = 1;

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
        wsContainer.connectToServer(this, URI.create(this.endpointUri));
    }

    public boolean isConnected() {
        return wsSession != null && wsSession.isOpen();
    }

    /**
     * Join a channel topic with a message payload and a channel callback
     *
     * @param topic
     * @param payload
     * @param callback
     *
     * @throws IOException
     */
    public Channel join(final String topic, final Payload payload, final ChannelCallback callback) throws IOException {
        LOG.log(Level.FINE, "join: {0}, {1}", new Object[]{topic, payload});
        final Channel channel = new Channel(topic, payload, callback, Socket.this);
        channels.add(channel);
        if(isConnected()) {
            rejoin(channel);
        }
        return channel;
    }

    /**
     * Join withoout a channel callback
     *
     * @param topic
     * @param payload
     * @throws IOException
     */
    public Channel join(final String topic, final Payload payload) throws IOException {
        return join(topic, payload, null);
    }

    /**
     * Join without a join message payload or a callback
     *
     * @param topic
     * @throws IOException
     */
    public Channel join(final String topic) throws IOException {
        return join(topic, null, null);
    }

    public void leave(final String topic) throws IOException {
        LOG.log(Level.FINE, "leave: {0}", topic);
        final Payload leavingPayload = new Payload(null);
        final Envelope envelope = new Envelope(topic, ChannelEvent.LEAVE.getPhxEvent(), leavingPayload, makeRef());
        send(envelope);
        for(final Iterator<Channel> channelIter = channels.iterator(); channelIter.hasNext(); channelIter.next()) {
            if(channelIter.next().isMember(topic)) {
                channelIter.remove();
            }
        }
    }

    public void rejoinAll() throws IOException {
        LOG.log(Level.FINE, "rejoinAll");
        for(final Channel channel: channels) {
            rejoin(channel);
        }
    }

    /**
     * TODO - Propagate exception differently
     *
     * @param envelope
     * @throws IOException
     */
    public void send(final Envelope envelope) throws IOException {
        LOG.log(Level.FINE, "Sending envelope: {0}", envelope);
        final ObjectNode node = objectMapper.createObjectNode();
        node.put("topic", envelope.getTopic());
        node.put("event", envelope.getEvent());
        node.put("ref", makeRef());

        if(envelope.getPayload() != null) {
            // TODO - Check if null check is sufficient
            node.putPOJO("payload", envelope.getPayload());
        }
        final String json = objectMapper.writeValueAsString(node);
        LOG.log(Level.FINE, "Sending JSON: {0}", json);
        wsSession.getAsyncRemote().sendText(json);
    }

    /**
     * Register a callback for SocketEvent.OPEN events
     *
     * @param callback
     */
    public void onOpen(final SocketCallback callback ) {
        this.socketEventCallbacks.get(SocketEvent.OPEN).add(callback);
    }

    /**
     * Register a callback for SocketEvent.ERROR events
     *
     * @param callback
     */
    public void onClose(final SocketCallback callback ) {
        this.socketEventCallbacks.get(SocketEvent.CLOSE).add(callback);

    }

    /**
     * Register a callback for SocketEvent.ERROR events
     *
     * @param callback
     */
    public void onError(final SocketCallback callback ) {
        this.socketEventCallbacks.get(SocketEvent.ERROR).add(callback);
    }

    /**
     * Register a callback for SocketEvent.MESSAGE events
     *
     * @param callback
     */
    public void onMessage(final SocketCallback callback ) {
        this.socketEventCallbacks.get(SocketEvent.MESSAGE).add(callback);
    }


    @OnOpen
    public void onConnOpen(final Session session) {
        LOG.log(Level.FINE, "WebSocket onOpen: {0}", session);
        this.wsSession = session;
        if(this.reconnectTimerTask != null) {
            this.reconnectTimerTask.cancel();
        }

        try {
            rejoinAll();
            for(final SocketCallback callback : socketEventCallbacks.get(SocketEvent.OPEN)) {
                callback.onOpen();
            }
        } catch (IOException e) {
            // TODO - logger, error callback
            e.printStackTrace();
        }
    }

    @OnClose
    public void onConnClose(final Session session, final CloseReason reason) {
        LOG.log(Level.FINE, "WebSocket onClose {0}, {1}", new Object[]{session, reason});
        this.wsSession = null;
        if(this.reconnectTimerTask != null) {
            this.reconnectTimerTask.cancel();
        }

        this.reconnectTimerTask = new TimerTask() {
            @Override
            public void run() {
                LOG.log(Level.FINE, "reconnectTimerTask run");
                try {
                    Socket.this.connect();
                } catch (IOException e) {
                    e.printStackTrace();
                    // TODO - log error, callback
                } catch (DeploymentException e) {
                    e.printStackTrace();
                }
            }
        };
        reconnectTimer.schedule(this.reconnectTimerTask, RECONNECT_INTERVAL_MS, RECONNECT_INTERVAL_MS);

        for(final SocketCallback callback : socketEventCallbacks.get(SocketEvent.CLOSE)) {
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
                    channel.trigger(envelope.getEvent(), envelope.getPayload());
                }
            }

            for(final SocketCallback callback : socketEventCallbacks.get(SocketEvent.MESSAGE)) {
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
        for(final PhxCallback callback : socketEventCallbacks.get(SocketEvent.CLOSE)) {
            callback.onError(error.toString()/* TODO - Throwable? */);
        }
    }

    synchronized String makeRef() {
        int val = refNo++;
        if(refNo == Integer.MAX_VALUE) {
            refNo = 0;
        }
        return Integer.toString(val);
    }

    static String replyEventName(final String ref) {
        return "chan_reply_" + ref;
    }

    private void rejoin(final Channel channel) throws IOException {
        LOG.log(Level.FINE, "join: {0}", channel);
        channel.reset();
        final Payload joinPayload = new Payload(null);
        final Envelope envelope = new Envelope(channel.getTopic(), ChannelEvent.JOIN.getPhxEvent(), joinPayload, makeRef());
        send(envelope);
    }


}