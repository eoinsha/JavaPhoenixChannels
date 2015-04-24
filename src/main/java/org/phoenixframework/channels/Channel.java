package org.phoenixframework.channels;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

public class Channel {
    private static final Logger LOG = Logger.getLogger(Channel.class.getName());

    @Override
    public String toString() {
        return "Channel{" +
                "topic='" + topic + '\'' +
                ", message=" + message +
                ", socket=" + socket +
                ", bindings=" + bindings +
                '}';
    }

    private String topic;
    private Message message;
    private PhoenixSocket socket;
    private List<Binding> bindings = new ArrayList<Binding>();

    public Channel(final String topic, final Message message, final PhoenixSocket socket) {
        this.topic = topic;
        this.message = message;
        this.socket = socket;
    }

    public String getTopic() {
        return topic;
    }

    public PhoenixSocket getSocket() {
        return socket;
    }

    public void reset() {
        this.bindings.clear();
    }

    public void on(final String event, final IPhoenixChannelCallback callback) {
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
                binding.getCallback().handleMessage(message);
                break;
            }
        }
    }

    public void send(final String event, final Message message) throws IOException {
        final Payload payload = new Payload(this.topic, event, message);
        socket.send(payload);
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