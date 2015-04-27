package org.phoenixframework.channels;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 *
 * // TODO - Figure out the reason for after(ms, callback) in phoenix.js
 */
public class Channel {
    private static final Logger LOG = Logger.getLogger(Channel.class.getName());

    @Override
    public String toString() {
        return "Channel{" +
                "topic='" + topic + '\'' +
                ", message=" + message +
                ", bindings=" + bindings +
                '}';
    }

    private String topic;
    private Message message;
    private PhxCallback callback;
    private PhxSocket socket;
    private List<Binding> bindings = new ArrayList<Binding>();
    // TODO - @chrismccord  phoenix.js has unused recHooks in class Channel
    private Push joinPush;

    /**
     * TODO - Is IPhoenixChannelCallback suitable for this?!
     */
    private Map<String, PhxCallback> receiveHooks = new HashMap<String, PhxCallback>();

    public Channel(final String topic, final Message message, final PhxCallback callback, final PhxSocket socket) {
        this.topic = topic;
        this.message = message;
        this.callback = callback;
        this.socket = socket;
        this.joinPush = new Push(this, ChannelEvent.JOIN.getPhxEvent(), message);
    }


    /**
     * @param status
     * @param callback
     * @return The instance's self
     */
    public Channel receive(final String status, final PhxCallback callback) {
        this.joinPush.receive(status, callback);
        return this;
    }

    public void onClose(final PhxCallback callback) {
        this.on(ChannelEvent.CLOSE.getPhxEvent(), callback);
    }

    /**
     * Register an error callback for the channel
     *
     * @param callback
     */
    public void onError(final PhxCallback callback) {
        this.on(ChannelEvent.ERROR.getPhxEvent(), new PhxCallback() {
            @Override
            public void onError(String reason) {
                callback.onError(reason);
                Channel.this.trigger(ChannelEvent.CLOSE.getPhxEvent(), reason);
            }
        });
    }

    /**
     * @param event
     * @param callback
     * @return The instance's self
     */
    public Channel on(final String event, final PhxCallback callback) {
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

    <T> void trigger(final String triggerEvent, final T message) {
        for(final Iterator<Binding> bindingIter = bindings.iterator(); bindingIter.hasNext();) {
            final Binding binding = bindingIter.next();
            if(binding.getEvent().equals(triggerEvent)) {
                binding.getCallback().onMessage(message);
                break;
            }
        }
    }

    /**
     * @param event
     * @param message
     * @return The instance's self
     * @throws IOException
     */
    public Channel send(final String event, final Message message) throws IOException {
        final Envelope envelope = new Envelope(this.topic, event, message, socket.makeRef());
        socket.send(envelope);
        return this;
    }

    /**
     * @return The instance's self
     */
    public Channel reset() {
        this.bindings.clear();
        final Push newJoinPush = new Push(this, ChannelEvent.JOIN.getPhxEvent(), this.message, this.joinPush);
        // this.onError Don't understand the need for rejoin on error in phoenix.js TODO
        this.on(ChannelEvent.REPLY.getPhxEvent(), new PhxCallback() {
            @Override
            public void onMessage(final Envelope envelope) {
                Channel.this.trigger(PhxSocket.replyEventName(envelope.getRef()), envelope);
            }
        });
        return this;
    }

    public void rejoin() throws IOException {
        this.reset();
        this.joinPush.send();
    }

    public Push push(final String event, final Message message) throws IOException {
        final Push pushEvent = new Push(this, event, message);
        pushEvent.send();
        return pushEvent;
    }

    public Push push(final String event) throws IOException {
        return push(event, null);
    }

    public Push leave() throws IOException {
        // TODO - Understsand the "ok" received here
        return this.push(ChannelEvent.LEAVE.getPhxEvent()).receive("ok", new PhxCallback() {
            @Override
            public void onMessage(Envelope message) {
                try {
                    Channel.this.getSocket().leave(/*TODO*/message.getTopic());
                } catch (IOException e) {
                    /* TODO */ e.printStackTrace();
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

    public PhxSocket getSocket() {
        return socket;
    }


}