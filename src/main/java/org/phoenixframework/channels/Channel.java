package org.phoenixframework.channels;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

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
    private PhxSocket socket;
    private List<Binding> bindings = new ArrayList<Binding>();

    /**
     * TODO - Is IPhoenixChannelCallback suitable for this?!
     */
    private Map<String, PhxCallback> receiveHooks = new HashMap<String, PhxCallback>();

    public Channel(final String topic, final Message message, final PhxSocket socket) {
        this.topic = topic;
        this.message = message;
        this.socket = socket;
    }

    public String getTopic() {
        return topic;
    }

    public PhxSocket getSocket() {
        return socket;
    }

    public void reset() {
        this.bindings.clear();
    }

    public void on(final String event, final PhxCallback callback) {
        this.bindings.add(new Binding(event, callback));
    }

    public void off(final String event) {
        for(final Iterator<Binding> bindingIter = bindings.iterator(); bindingIter.hasNext();) {
            if(bindingIter.next().getEvent().equals(event)) {
                bindingIter.remove();
                break;
            }
        }
    }

    public void trigger(final String triggerEvent, final Message message) {
        for(final Iterator<Binding> bindingIter = bindings.iterator(); bindingIter.hasNext();) {
            final Binding binding = bindingIter.next();
            if(binding.getEvent().equals(triggerEvent)) {
                binding.getCallback().onMessage(message);
                break;
            }
        }
    }

    public void send(final String event, final Message message) throws IOException {
        final Envelope envelope = new Envelope(this.topic, event, message, socket.makeRef());
        socket.send(envelope);
    }

    public boolean isMember(final String topic) {
        return this.topic == topic;
    }

    public void leave() throws IOException {
        if(socket != null) {
            socket.leave(this.topic);
        }
    }
}