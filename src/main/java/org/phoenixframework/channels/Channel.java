package org.phoenixframework.channels;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * // TODO - Figure out the reason for after(ms, callback) in phoenix.js - ask @chrismccord
 */
public class Channel {
    private static final Logger LOG = Logger.getLogger(Channel.class.getName());


    private String topic;
    private Payload payload;
    private ChannelCallback callback;
    private Socket socket;
    private List<Binding> bindings = new ArrayList<>();
    // TODO - @chrismccord  phoenix.js has unused recHooks in class Channel
    private Push joinPush;

    private Map<String, PhxCallback> receiveHooks = new HashMap<>();

    private Timer rejoinTimer = null;
    private TimerTask rejoinTimerTask = null;

    public Channel(final String topic, final Payload payload, final ChannelCallback callback, final Socket socket) {
        this.topic = topic;
        this.payload = payload;
        this.callback = callback;
        this.socket = socket;
        this.joinPush = new Push(this, ChannelEvent.JOIN.getPhxEvent(), payload);
        this.rejoinTimer = new Timer("Rejoin timer for " + topic);
    }

    /**
     * @param status
     * @param callback
     * @return The instance's self
     */
    public Channel receive(final String status, final ChannelCallback callback) {
        this.joinPush.receive(status, callback);
        return this;
    }

    public void onClose(final ChannelCallback callback) {
        this.on(ChannelEvent.CLOSE.getPhxEvent(), callback);
    }

    /**
     * Register an error callback for the channel
     *
     * @param callback
     */
    public void onError(final PhxCallback callback) {
        this.on(ChannelEvent.ERROR.getPhxEvent(), new ChannelCallback() {
            @Override
            public void onError(final String reason) {
                callback.onError(reason);
                Channel.this.trigger(ChannelEvent.CLOSE.getPhxEvent(), new Payload(reason));
            }
        });
    }

    /**
     * @param event
     * @param callback
     * @return The instance's self
     */
    public Channel on(final String event, final ChannelCallback callback) {
        synchronized(bindings) {
            this.bindings.add(new Binding(event, callback));
        }
        return this;
    }

    /**
     * @param event
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

    void trigger(final String triggerEvent, final Payload payload) {
        synchronized(bindings) {
            for (final Iterator<Binding> bindingIter = bindings.iterator(); bindingIter.hasNext(); ) {
                final Binding binding = bindingIter.next();
                if (binding.getEvent().equals(triggerEvent)) {
                    binding.getCallback().onMessage(Channel.this.topic, triggerEvent, payload);
                }
            }
        }
    }

    void trigger(final String triggerEvent, final Envelope envelope) {
        if (ChannelEvent.valueOf(triggerEvent) != null) {
            trigger(triggerEvent, envelope.getPayload());
        } else {
            synchronized(bindings) {
                for (final Iterator<Binding> bindingIter = bindings.iterator(); bindingIter.hasNext(); ) {
                    final Binding binding = bindingIter.next();
                    if (binding.getEvent().equals(triggerEvent)) {
                        // Channel Events get the full envelope
                        binding.getCallback().onMessage(envelope);
                    }
                    break;
                }
            }
        }
    }

    /**
     * @param event
     * @param payload
     * @return The instance's self
     * @throws IOException
     */
    public Channel send(final String event, final Payload payload) throws IOException {
        final Envelope envelope = new Envelope(this.topic, event, payload, socket.makeRef());
        socket.send(envelope);
        return this;
    }

    /**
     * @return The instance's self
     */
    public Channel reset() {
        synchronized(bindings) {
            this.bindings.clear();
        }
        final Push newJoinPush = new Push(this, ChannelEvent.JOIN.getPhxEvent(), this.payload, this.joinPush);
        this.onError(new ChannelCallback(){
            /* TODO - @chrismccord Figure out potentital contention betweeen this timer and the Socket reconenct timer */
            @Override
            public void onError(String reason) {
                super.onError(reason);
                Channel.this.rejoinTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            Channel.this.rejoin();
                        } catch (IOException e) {
                            e.printStackTrace();
                            /* TODO Log and/or signal upstream */
                        }
                    }
                };
                Channel.this.rejoinTimer.schedule(Channel.this.rejoinTimerTask, Socket.RECONNECT_INTERVAL_MS, Socket.RECONNECT_INTERVAL_MS);
            }
        });
        this.on(ChannelEvent.REPLY.getPhxEvent(), new ChannelCallback() {
            @Override
            public void onMessage(final Envelope envelope) {
                // TODO @chrismccord What's the actual point of replyEventName
                Channel.this.trigger(Socket.replyEventName(envelope.getRef()), envelope);
            }
        });
        return this;
    }

    public void rejoin() throws IOException {
        this.reset();
        this.joinPush.send();
    }

    public Push push(final String event, final Payload payload) throws IOException {
        final Push pushEvent = new Push(this, event, payload);
        pushEvent.send();
        return pushEvent;
    }

    public Push push(final String event) throws IOException {
        return push(event, null);
    }

    public Push leave() throws IOException {
        // TODO - Understand the "ok" received here
        return this.push(ChannelEvent.LEAVE.getPhxEvent()).receive("ok", new ChannelCallback() {
            @Override
            public void onMessage(final String topic, final String event, final Payload payload) {
                try {
                    Channel.this.getSocket().leave(topic);
                } catch (IOException e) {
                    /* TODO Log and/or signal upstream */
                    e.printStackTrace();
                }
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

    @Override
    public String toString() {
        return "Channel{" +
                "topic='" + topic + '\'' +
                ", message=" + payload +
                ", bindings=" + bindings +
                '}';
    }
}
