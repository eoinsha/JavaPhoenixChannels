package org.phoenixframework.channels;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulation of a Phoenix channel: a Socket, a topic and the channel's state.
 */
public class Channel {
    private static final Logger LOG = Logger.getLogger(Channel.class.getName());

    private String topic;
    private JsonNode payload;
    private Socket socket;
    private final List<Binding> bindings = new ArrayList<>();
    private Push joinPush;

    private Timer channelTimer = null;

    private boolean joinedOnce = false;
    private ChannelState state = ChannelState.CLOSED;
    public static long DEFAULT_TIMEOUT = 5000;

    private ConcurrentLinkedDeque<Push> pushBuffer = new ConcurrentLinkedDeque<>();

    public Channel(final String topic, final JsonNode payload, final Socket socket) {
        this.topic = topic;
        this.payload = payload;
        this.socket = socket;
        this.joinPush = new Push(this, ChannelEvent.JOIN.getPhxEvent(), payload, DEFAULT_TIMEOUT);
        this.channelTimer = new Timer("Phx Rejoin timer for " + topic);

        this.joinPush.receive("ok", new IMessageCallback() {
            @Override
            public void onMessage(Envelope envelope) {
                Channel.this.state = ChannelState.JOINED;
            }
        });
        this.onClose(new IMessageCallback() {
            @Override
            public void onMessage(Envelope envelope) {
                Channel.this.state = ChannelState.CLOSED;
                Channel.this.socket.remove(Channel.this);
            }
        });
        this.onError(new IErrorCallback() {
            @Override
            public void onError(String reason) {
                Channel.this.state = ChannelState.ERRORED;
                scheduleRejoinTimer();
            }
        });
        this.on(ChannelEvent.REPLY.getPhxEvent(), new IMessageCallback() {
            @Override
            public void onMessage(final Envelope envelope) {
                Channel.this.trigger(Socket.replyEventName(envelope.getRef()), envelope);
            }
        });
    }

    public void rejoinUntilConnected() throws IOException {
        if(this.state == ChannelState.ERRORED) {
            if(this.socket.isConnected()) {
                this.rejoin();
            }
            else {
                scheduleRejoinTimer();
            }
        }
    }

    /**
     * Initiates a channel join event
     *
     * @throws IllegalStateException Thrown if the channel has already been joined
     * @throws IOException Thrown if the join could not be sent
     *
     * @return This Push instance
     */
    public Push join() throws IllegalStateException, IOException {
        if(this.joinedOnce) {
            throw new IllegalStateException("Tried to join multiple times. 'join' can only be invoked once per channel");
        }
        this.joinedOnce = true;
        this.sendJoin();
        return this.joinPush;
    }

    public void onClose(final IMessageCallback callback) {
        this.on(ChannelEvent.CLOSE.getPhxEvent(), callback);
    }


    /**
     * Register an error callback for the channel
     *
     * @param callback Callback to be invoked on error
     */
    public void onError(final IErrorCallback callback) {
        this.on(ChannelEvent.ERROR.getPhxEvent(), new IMessageCallback() {
            @Override
            public void onMessage(final Envelope envelope) {
                String reason = null;
                if (envelope != null) {
                    reason = envelope.getReason();
                }
                callback.onError(reason);
            }
        });
    }

    /**
     * @param event The event name
     * @param callback The callback to be invoked with the event's message
     *
     * @return The instance's self
     */
    public Channel on(final String event, final IMessageCallback callback) {
        synchronized(bindings) {
            this.bindings.add(new Binding(event, callback));
        }
        return this;
    }

    /**
     * Unsubscribe for event notifications
     *
     * @param event The event name
     * @return The instance's self
     */
    public Channel off(final String event) {
        synchronized(bindings) {
            for (final Iterator<Binding> bindingIter = bindings.iterator(); bindingIter.hasNext(); ) {
                if (bindingIter.next().getEvent().equals(event)) {
                    bindingIter.remove();
                    break;
                }
            }
        }
        return this;
    }

    /**
     * Triggers event signalling to all callbacks bound to the specified event.
     *
     * @param triggerEvent The event name
     * @param envelope The message's envelope relating to the event or null if not relevant.
     */
    void trigger(final String triggerEvent, final Envelope envelope) {
        synchronized(bindings) {
            for (final Binding binding : bindings) {
                if (binding.getEvent().equals(triggerEvent)) {
                    // Channel Events get the full envelope
                    binding.getCallback().onMessage(envelope);
                    break;
                }
            }
        }
    }

    public void rejoin() throws IOException {
        this.sendJoin();
        while(!this.pushBuffer.isEmpty()) {
            this.pushBuffer.removeFirst().send();
        }
    }

    /**
     * @return true if the socket is open and the channel has joined
     */
    public boolean canPush() {
        return this.socket.isConnected() && this.state == ChannelState.JOINED;
    }

    /**
     * Pushes a payload to be sent to the channel
     *
     * @param event The event name
     * @param payload The message payload
     * @param timeout The number of milliseconds to wait before triggering a timeout
     *
     * @return The Push instance used to send the message
     *
     * @throws IOException Thrown if the payload cannot be pushed
     * @throws IllegalStateException Thrown if the channel has not yet been joined
     */
    public Push push(final String event, final JsonNode payload, final long timeout) throws IOException, IllegalStateException {
        if(!this.joinedOnce) {
            throw new IllegalStateException("Unable to push event before channel has been joined");
        }
        final Push pushEvent = new Push(this, event, payload, timeout);
        if(this.canPush()) {
            pushEvent.send();
        }
        else {
            this.pushBuffer.add(pushEvent);
        }
        return pushEvent;
    }

    public Push push(final String event, final JsonNode payload) throws IOException {
        return push(event, payload, DEFAULT_TIMEOUT);
    }

    public Push push(final String event) throws IOException {
        return push(event, null);
    }

    public Push leave() throws IOException {
        return this.push(ChannelEvent.LEAVE.getPhxEvent()).receive("ok", new IMessageCallback() {
            public void onMessage(final Envelope envelope) {
                Channel.this.trigger(ChannelEvent.CLOSE.getPhxEvent(), null);
            }
        });
    }

    public boolean isMember(final String topic) {
        return this.topic.equals(topic);
    }

    public String getTopic() {
        return topic;
    }

    public Socket getSocket() {
        return socket;
    }

    public void scheduleRepeatingTask(TimerTask timerTask, long ms) {
        this.channelTimer.schedule(timerTask, ms, ms);
    }

    public void scheduleTask(TimerTask timerTask, long ms) {
        this.channelTimer.schedule(timerTask, ms);
    }


    private void sendJoin() throws IOException {
        this.state = ChannelState.JOINING;
        this.joinPush.send();
    }

    @Override
    public String toString() {
        return "Channel{" +
            "topic='" + topic + '\'' +
            ", message=" + payload +
            ", bindings=" + bindings +
            '}';
    }

    private void scheduleRejoinTimer() {
        final TimerTask rejoinTimerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    Channel.this.rejoinUntilConnected();
                } catch (IOException e) {
                    LOG.log(Level.SEVERE, "Failed to rejoin", e);
                }
            }
        };
        scheduleTask(rejoinTimerTask, Socket.RECONNECT_INTERVAL_MS);
    }


}
