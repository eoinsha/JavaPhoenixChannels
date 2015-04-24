package org.phoenixframework.channels;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.*;

@ClientEndpoint
public class PhoenixSocket {
    private static final Logger LOG = Logger.getLogger(PhoenixSocket.class.getName());

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

    private LinkedBlockingQueue<Payload> resendQueue = new LinkedBlockingQueue<Payload>(MAX_RESEND_MESSAGES);

    private int refNo = 1;

    public PhoenixSocket(final String endpointUri) throws IOException, DeploymentException {
        LOG.log(Level.FINE, "PhoenixSocket({0})", endpointUri);
        this.endpointUri = endpointUri;
        this.reconnectTimer = new Timer("Reconnect Timer for " + endpointUri);
    }

    public void close() throws IOException {
        LOG.log(Level.FINE, "close");
        if(wsSession != null && wsSession.isOpen()) {
            wsSession.close();
        }
    }

    public void connect() throws IOException, DeploymentException {
        LOG.log(Level.FINE, "connect");
        close();
        wsContainer.connectToServer(this, URI.create(this.endpointUri));
    }

    public boolean isConnected() {
        return wsSession != null && wsSession.isOpen();
    }

    public void rejoinAll() throws IOException {
        LOG.log(Level.FINE, "rejoinAll");
        for(final Channel channel: channels) {
            rejoin(channel);
        }
    }

    public void rejoin(final Channel channel) throws IOException {
        LOG.log(Level.FINE, "rejoin: {0}", channel);
        channel.reset();
        final Message joinMessage = new Message(null);
        final Payload payload = new Payload(channel.getTopic(), ChannelEvent.JOIN.getPhxEvent(), joinMessage, getRef());
        send(payload);
    }

    public void join(final String topic, final Message message) throws IOException {
        LOG.log(Level.FINE, "join: {0}, {1}", new Object[]{ topic, message });
        final Channel channel = new Channel(topic, message, PhoenixSocket.this);
        channels.add(channel);
        if(isConnected()) {
            rejoin(channel);
        }
    }

    public void leave(final String topic) throws IOException {
        LOG.log(Level.FINE, "leave: {0}", topic);
        final Message leavingMessage = new Message(null);
        final Payload payload = new Payload(topic, ChannelEvent.LEAVE.getPhxEvent(), leavingMessage, getRef());
        send(payload);
        for(final Iterator<Channel> channelIter = channels.iterator(); channelIter.hasNext(); channelIter.next()) {
            if(channelIter.next().isMember(topic)) {
                channelIter.remove();
            }
        }
    }

    /**
     * TODO - Propagate exception differently
     *
     * @param payload
     * @throws IOException
     */
    public void send(final Payload payload) throws IOException {
        LOG.log(Level.FINE, "Sending payload: {0}", payload);
        final ObjectNode node = objectMapper.createObjectNode();
        node.put("topic", payload.getTopic());
        node.put("event", payload.getEvent());
        node.put("ref", getRef());

        if(payload.getMessage() != null) {
            // TODO - Check if null check is sufficient
            node.putPOJO("payload",payload.getMessage());
        }
        final String json = objectMapper.writeValueAsString(node);
        LOG.log(Level.FINE, "Sending JSON: {0}", json);
        wsSession.getAsyncRemote().sendText(json);
    }

    synchronized String getRef() {
        int val = refNo++;
        if(refNo == Integer.MAX_VALUE) {
            refNo = 0;
        }
        return Integer.toString(val);
    }

    public static String replyEventName(final int refNo) {
        return "chan_reply_" + refNo;
    }

    @OnOpen
    public void onOpen(final Session session) {
        LOG.log(Level.FINE, "WebSocket onOpen: {0}", session);
        this.wsSession = session;
        if(this.reconnectTimerTask != null) {
            this.reconnectTimerTask.cancel();
        }

        try {
            rejoinAll();
        } catch (IOException e) {
            // TODO - logger, error callback
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(final Session session, final CloseReason reason) {
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
                    PhoenixSocket.this.connect();
                } catch (IOException e) {
                    e.printStackTrace();
                    // TODO - log error, callback
                } catch (DeploymentException e) {
                    e.printStackTrace();
                }
            }
        };

        reconnectTimer.schedule(this.reconnectTimerTask, RECONNECT_INTERVAL_MS, RECONNECT_INTERVAL_MS);
    }

    @OnMessage
    public void onTextMessage(final String payloadText) {
        LOG.log(Level.FINE, "Payload received: {0}", payloadText);
        try {
            final Payload payload = objectMapper.readValue(payloadText, Payload.class);
            for(final Channel channel : channels) {
                if(channel.isMember(payload.getTopic())) {
                    channel.trigger(payload.getEvent(), payload.getMessage());
                }
            }
        } catch (IOException e) {
            // TODO: log, signal
            e.printStackTrace();
        }
    }
}