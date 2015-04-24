package org.phoenixframework.channels;

public class Binding {
    final String event;
    final IPhoenixChannelCallback callback;

    public Binding(final String event, final IPhoenixChannelCallback callback) {
        this.event = event;
        this.callback = callback;
    }

    public String getEvent() {
        return event;
    }

    public IPhoenixChannelCallback getCallback() {
        return callback;
    }
}
