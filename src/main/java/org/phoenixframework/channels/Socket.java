package org.phoenixframework.channels;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.ws.WebSocket;
import okhttp3.ws.WebSocketCall;
import okhttp3.ws.WebSocketListener;
import okio.Buffer;

public class Socket {

    public class PhoenixWSListener implements WebSocketListener {

        @Override
        public void onClose(final int code, final String reason) {
            LOG.log(Level.FINE, "WebSocket onClose {0}/{1}", new Object[]{code, reason});
            cancelReconnectTimer();
            cancelHeartbeatTimer();
            Socket.this.webSocket = null;

            for (final ISocketCloseCallback callback : socketCloseCallbacks) {
                callback.onClose();
            }
        }

        @Override
        public void onFailure(final IOException e, final Response response) {
            LOG.log(Level.WARNING, "WebSocket connection error", e);
            try {
                for (final IErrorCallback callback : errorCallbacks) {

                    //TODO if there are multiple errorCallbacks do we really want to trigger
                    //the same channel error callbacks multiple times?
                    triggerChannelError();
                    callback.onError(e.toString());
                }
            } finally {
                cancelReconnectTimer();
                cancelHeartbeatTimer();

                // Assume closed on failure
                if (Socket.this.webSocket != null) {
                    try {
                        Socket.this.webSocket.close(1001 /*CLOSE_GOING_AWAY*/, "EOF received");
                    } catch (IOException ioe) {
                        LOG.log(Level.WARNING, "Failed to explicitly close following failure");
                    } finally {
                        Socket.this.webSocket = null;
                    }
                }
                if (reconnectOnFailure) {
                    scheduleReconnectTimer();
                }
            }
        }

        @Override
        public void onMessage(final ResponseBody payload) throws IOException {
            LOG.log(Level.FINE, "Envelope received: {0}", payload);

            try {
                if (payload.contentType() == WebSocket.TEXT) {
                    final Envelope envelope =
                            objectMapper.readValue(payload.byteStream(), Envelope.class);
                    synchronized (channels) {
                        for (final Channel channel : channels) {
                            if (channel.isMember(envelope.getTopic())) {
                                channel.trigger(envelope.getEvent(), envelope);
                            }
                        }
                    }

                    for (final IMessageCallback callback : messageCallbacks) {
                        callback.onMessage(envelope);
                    }
                }
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Failed to read message payload", e);
            } finally {
                payload.close();
            }
        }

        @Override
        public void onOpen(final WebSocket webSocket, final Response response) {
            LOG.log(Level.FINE, "WebSocket onOpen: {0}", webSocket);
            Socket.this.webSocket = webSocket;
            cancelReconnectTimer();

            startHeartbeatTimer();

            for (final ISocketOpenCallback callback : socketOpenCallbacks) {
                callback.onOpen();
            }

            Socket.this.flushSendBuffer();
        }

        @Override
        public void onPong(final Buffer payload) {
            LOG.log(Level.INFO, "PONG received: {0}", payload);
        }
    }

    private static final Logger LOG = Logger.getLogger(Socket.class.getName());

    public static final int RECONNECT_INTERVAL_MS = 5000;

    private static final int DEFAULT_HEARTBEAT_INTERVAL = 7000;

    private final List<Channel> channels = new ArrayList<>();

    private String endpointUri = null;

    private final Set<IErrorCallback> errorCallbacks = Collections
            .newSetFromMap(new HashMap<IErrorCallback, Boolean>());

    private final int heartbeatInterval;

    private TimerTask heartbeatTimerTask = null;

    private final OkHttpClient httpClient = new OkHttpClient();

    private final Set<IMessageCallback> messageCallbacks = Collections
            .newSetFromMap(new HashMap<IMessageCallback, Boolean>());

    private final ObjectMapper objectMapper = new ObjectMapper();

    private boolean reconnectOnFailure = true;

    private TimerTask reconnectTimerTask = null;

    private int refNo = 1;

    private final LinkedBlockingQueue<RequestBody> sendBuffer = new LinkedBlockingQueue<>();

    private final Set<ISocketCloseCallback> socketCloseCallbacks = Collections
            .newSetFromMap(new HashMap<ISocketCloseCallback, Boolean>());

    private final Set<ISocketOpenCallback> socketOpenCallbacks = Collections
            .newSetFromMap(new HashMap<ISocketOpenCallback, Boolean>());

    private Timer timer = null;

    private WebSocket webSocket = null;

    /**
     * Annotated WS Endpoint. Private member to prevent confusion with "onConn*" registration
     * methods.
     */
    private final PhoenixWSListener wsListener = new PhoenixWSListener();

    public Socket(final String endpointUri) throws IOException {
        this(endpointUri, DEFAULT_HEARTBEAT_INTERVAL);
    }

    public Socket(final String endpointUri, final int heartbeatIntervalInMs) {
        LOG.log(Level.FINE, "PhoenixSocket({0})", endpointUri);
        this.endpointUri = endpointUri;
        this.heartbeatInterval = heartbeatIntervalInMs;
        this.timer = new Timer("Reconnect Timer for " + endpointUri);
    }

    /**
     * Retrieve a channel instance for the specified topic
     *
     * @param topic   The channel topic
     * @param payload The message payload
     * @return A Channel instance to be used for sending and receiving events for the topic
     */
    public Channel chan(final String topic, final JsonNode payload) {
        LOG.log(Level.FINE, "chan: {0}, {1}", new Object[]{topic, payload});
        final Channel channel = new Channel(topic, payload, Socket.this);
        synchronized (channels) {
            channels.add(channel);
        }
        return channel;
    }

    public void connect() throws IOException {
        LOG.log(Level.FINE, "connect");
        disconnect();
        // No support for ws:// or ws:// in okhttp. See https://github.com/square/okhttp/issues/1652
        final String httpUrl = this.endpointUri.replaceFirst("^ws:", "http:")
                .replaceFirst("^wss:", "https:");
        final Request request = new Request.Builder().url(httpUrl).build();
        final WebSocketCall wsCall = WebSocketCall.create(httpClient, request);
        wsCall.enqueue(wsListener);
    }

    public void disconnect() throws IOException {
        cancelReconnectTimer();
        cancelHeartbeatTimer();

        LOG.log(Level.FINE, "disconnect");
        if (webSocket != null) {
            webSocket.close(1001 /*CLOSE_GOING_AWAY*/, "Disconnected by client");
        }
    }

    /**
     * @return true if the socket connection is connected
     */
    public boolean isConnected() {
        return webSocket != null;
    }

    /**
     * Register a callback for SocketEvent.ERROR events
     *
     * @param callback The callback to receive CLOSE events
     * @return This Socket instance
     */
    public Socket onClose(final ISocketCloseCallback callback) {
        this.socketCloseCallbacks.add(callback);
        return this;
    }

    /**
     * Register a callback for SocketEvent.ERROR events
     *
     * @param callback The callback to receive ERROR events
     * @return This Socket instance
     */
    public Socket onError(final IErrorCallback callback) {
        this.errorCallbacks.add(callback);
        return this;
    }

    /**
     * Register a callback for SocketEvent.MESSAGE events
     *
     * @param callback The callback to receive MESSAGE events
     * @return This Socket instance
     */
    public Socket onMessage(final IMessageCallback callback) {
        this.messageCallbacks.add(callback);
        return this;
    }

    /**
     * Register a callback for SocketEvent.OPEN events
     *
     * @param callback The callback to receive OPEN events
     * @return This Socket instance
     */
    public Socket onOpen(final ISocketOpenCallback callback) {
        cancelReconnectTimer();
        this.socketOpenCallbacks.add(callback);
        return this;
    }

    /**
     * Sends a message envelope on this socket
     *
     * @param envelope The message envelope
     * @return This socket instance
     * @throws IOException Thrown if the message cannot be sent
     */
    public Socket push(final Envelope envelope) throws IOException {
        LOG.log(Level.FINE, "Pushing envelope: {0}", envelope);
        final ObjectNode node = objectMapper.createObjectNode();
        node.put("topic", envelope.getTopic());
        node.put("event", envelope.getEvent());
        node.put("ref", envelope.getRef());
        node.set("payload", envelope.getPayload() == null ? objectMapper.createObjectNode()
                : envelope.getPayload());
        final String json = objectMapper.writeValueAsString(node);
        LOG.log(Level.FINE, "Sending JSON: {0}", json);

        RequestBody body = RequestBody.create(WebSocket.TEXT, json);

        if (this.isConnected()) {
            try {
                webSocket.sendMessage(body);
            } catch (IllegalStateException e) {
                LOG.log(Level.SEVERE, "Attempted to send push when socket is not open", e);
            }
        } else {
            this.sendBuffer.add(body);
        }

        return this;
    }

    /**
     * Should the socket attempt to reconnect if websocket.onFailure is called.
     *
     * @param reconnectOnFailure reconnect value
     */
    public void reconectOnFailure(final boolean reconnectOnFailure) {
        this.reconnectOnFailure = reconnectOnFailure;
    }

    /**
     * Removes the specified channel if it is known to the socket
     *
     * @param channel The channel to be removed
     */
    public void remove(final Channel channel) {
        synchronized (channels) {
            for (final Iterator chanIter = channels.iterator(); chanIter.hasNext(); ) {
                if (chanIter.next() == channel) {
                    chanIter.remove();
                    break;
                }
            }
        }
    }

    public void removeAllChannels() {
        synchronized (channels) {
            channels.clear();
        }
    }

    @Override
    public String toString() {
        synchronized (channels) {
            return "PhoenixSocket{" +
                    "endpointUri='" + endpointUri + '\'' +
                    ", channels=" + channels +
                    ", refNo=" + refNo +
                    ", webSocket=" + webSocket +
                    '}';
        }
    }

    synchronized String makeRef() {
        int val = refNo++;
        if (refNo == Integer.MAX_VALUE) {
            refNo = 0;
        }
        return Integer.toString(val);
    }

    private void cancelHeartbeatTimer() {
        if (Socket.this.heartbeatTimerTask != null) {
            Socket.this.heartbeatTimerTask.cancel();
        }
    }

    private void cancelReconnectTimer() {
        if (Socket.this.reconnectTimerTask != null) {
            Socket.this.reconnectTimerTask.cancel();
        }
    }

    private void flushSendBuffer() {
        while (this.isConnected() && !this.sendBuffer.isEmpty()) {
            final RequestBody body = this.sendBuffer.remove();
            try {
                this.webSocket.sendMessage(body);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Failed to send payload {0}", body);
            }
        }
    }

    /**
     * Sets up and schedules a timer task to make repeated reconnect attempts at configured
     * intervals
     */
    private void scheduleReconnectTimer() {
        cancelReconnectTimer();
        cancelHeartbeatTimer();

        Socket.this.reconnectTimerTask = new TimerTask() {
            @Override
            public void run() {
                LOG.log(Level.FINE, "reconnectTimerTask run");
                try {
                    Socket.this.connect();
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Failed to reconnect to " + Socket.this.wsListener, e);
                }
            }
        };
        timer.schedule(Socket.this.reconnectTimerTask, RECONNECT_INTERVAL_MS);
    }

    private void startHeartbeatTimer() {
        cancelHeartbeatTimer();

        Socket.this.heartbeatTimerTask = new TimerTask() {
            @Override
            public void run() {
                LOG.log(Level.FINE, "heartbeatTimerTask run");
                if (Socket.this.isConnected()) {
                    try {
                        Envelope envelope = new Envelope("phoenix", "heartbeat",
                                new ObjectNode(JsonNodeFactory.instance), Socket.this.makeRef());
                        Socket.this.push(envelope);
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "Failed to send heartbeat", e);
                    }
                }
            }
        };

        timer.schedule(Socket.this.heartbeatTimerTask, Socket.this.heartbeatInterval,
                Socket.this.heartbeatInterval);
    }

    private void triggerChannelError() {
        synchronized (channels) {
            for (final Channel channel : channels) {
                channel.trigger(ChannelEvent.ERROR.getPhxEvent(), null);
            }
        }
    }

    static String replyEventName(final String ref) {
        return "chan_reply_" + ref;
    }
}
