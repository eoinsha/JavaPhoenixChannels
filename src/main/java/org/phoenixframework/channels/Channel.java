package org.phoenixframework.channels;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * // TODO - Figure out the reason for after(ms, callback) in phoenix.js
 */
public class Channel {
    private static final Logger LOG = Logger.getLogger(Channel.class.getName());


    private String topic;
    private Payload payload;
    private ChannelCallback callback;
    private Socket socket;
    private List<Binding> bindings = new ArrayList<Binding>();
    // TODO - @chrismccord  phoenix.js has unused recHooks in class Channel
    private Push joinPush;

    private Map<String, PhxCallback> receiveHooks = new HashMap<String, PhxCallback>();

    public Channel(final String topic, final Payload payload, final ChannelCallback callback, final Socket socket) {
        this.topic = topic;
        this.payload = payload;
        this.callback = callback;
        this.socket = socket;
        this.joinPush = new Push(this, ChannelEvent.JOIN.getPhxEvent(), payload);
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
        this.bindings.add(new Binding(event, callback));
        return this;
    }

    /**
     * @param event
     * @return The instance's self
     */
    public Channel off(final String event) {
        for(final Iterator<Binding> bindingIter = bindings.iterator(); bindingIter.hasNext();) {
            if(bindingIter.next().getEvent().equals(event)) {
                bindingIter.remove();
                break;
            }
        }
        return this;
    }

    void trigger(final String triggerEvent, final Payload payload) {
        for(final Iterator<Binding> bindingIter = bindings.iterator(); bindingIter.hasNext();) {
            final Binding binding = bindingIter.next();
            if(binding.getEvent().equals(triggerEvent)) {
                binding.getCallback().onMessage(Channel.this.topic, triggerEvent, payload);
                break;
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
        this.bindings.clear();
        final Push newJoinPush = new Push(this, ChannelEvent.JOIN.getPhxEvent(), this.payload, this.joinPush);
        // this.onError Don't understand the need for rejoin on error in phoenix.js TODO
        this.on(ChannelEvent.REPLY.getPhxEvent(), new ChannelCallback() {
            @Override
            public void onMessage(final String event, final String topic, final Payload payload) {
                // TODO What's the actual point of replyEventName
                Channel.this.trigger(
                        /* Socket.replyEventName(envelope.getRef())*/
                        event /*?*/, payload);
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
                    Channel.this.getSocket().leave(/*TODO*/topic);
                } catch (IOException e) {
                    /* TODO */
                    e.printStackTrace();
                }
            }
        });
    }

    public boolean isMember(final String topic) {
        return this.topic == topic;
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
