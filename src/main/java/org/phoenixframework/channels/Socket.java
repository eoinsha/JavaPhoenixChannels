package org.phoenixframework.channels;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.Module;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;

/**
 *
 */
public class Socket extends WebSocketHandler {
    public static final int MAX_RESEND_MESSAGES = 50;

    private static final int RECONNECT_INTERVAL_MS = 5000;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebSocketConnection wsConnection = new WebSocketConnection();

    private String endpoint = null;
    private List<Channel> channels = new ArrayList<Channel>();

    private Timer reconnectTimer = null;
    private TimerTask reconnectTimerTask = null;

    private LinkedBlockingQueue<Payload> resendQueue = new LinkedBlockingQueue<Payload>(MAX_RESEND_MESSAGES);

    private int refNo = 1;

    public Socket(final String endpoint) throws WebSocketException {
        this.endpoint = endpoint;
        this.reconnectTimer = new Timer("Reconnect Timer for " + endpoint);

        reconnect();
    }

    public void close() throws WebSocketException {
        if(wsConnection.isConnected()) {
            wsConnection.disconnect();
        }
    }

    private void reconnect() throws WebSocketException {
        close();
        wsConnection.connect(endpoint, this);
    }

    public boolean isConnected() {
        return wsConnection.isConnected();
    }

    public void rejoinAll() throws IOException {
        for(final Channel channel: channels) {
            rejoin(channel);
        }
    }

    public void rejoin(final Channel channel) throws IOException {
        channel.reset();
        final Message joinMessage = new Message("status", "joining");
        final Payload payload = new Payload(channel.getChannel(), channel.getTopic(), "join", joinMessage);
        send(payload);
    }

    public void join(final String channelName/* TODO - NAME? */, final String topic, final Message message) throws IOException {
        final Channel channel = new Channel(channelName, topic, message, Socket.this);
        channels.add(channel);
        if(isConnected()) {
            rejoin(channel);
        }
    }

    public void leave(final String channel, final String topic) throws IOException {
        final Message leavingMessage = new Message("status", "leaving");
        final Payload payload = new Payload(channel, topic, "leave", leavingMessage);
        send(payload);
        for(final Iterator<Channel> channelIter = channels.iterator(); channelIter.hasNext(); channelIter.next()) {
            if(channelIter.next().isMember(channel, topic)) {
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
        final ObjectNode node = objectMapper.createObjectNode();
        node.put("topic", payload.getTopic());
        node.put("event", payload.getEvent());
        node.put("ref", getRefNo());

        if(payload.getMessage() != null) {
            // TODO - Check if null check is sufficient
            node.putPOJO("message",payload.getMessage());
        }
        wsConnection.sendTextMessage(objectMapper.writeValueAsString(node));
    }

    private synchronized int getRefNo() {
        int val = refNo++;
        if(refNo == Integer.MAX_VALUE) {
            refNo = 0;
        }
        return val;
    }

    public static String replyEventName(final int refNo) {
        return "chan_reply_" + refNo;
    }

    @Override
    public void onOpen() {
        this.reconnectTimerTask.cancel();
        super.onOpen();
        try {
            rejoinAll();
        } catch (IOException e) {
            // TODO - logger, error callback
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(final int code, final String reason) {
        this.reconnectTimerTask.cancel();
        super.onClose(code, reason);

        this.reconnectTimerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    Socket.this.reconnect();
                } catch (WebSocketException e) {
                    e.printStackTrace();
                    // TODO - log error, callback
                }
            }
        };

        reconnectTimer.schedule(this.reconnectTimerTask, RECONNECT_INTERVAL_MS, RECONNECT_INTERVAL_MS);
    }

    @Override
    public void onTextMessage(final String payload) {
        super.onTextMessage(payload);
    }
}