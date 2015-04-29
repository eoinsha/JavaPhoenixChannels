package org.phoenixframework.channels;

import org.phoenixframework.channels.PhxCallback;

class Binding {
    final String event;
    final ChannelCallback callback;

    public Binding(final String event, final ChannelCallback callback) {
        this.event = event;
        this.callback = callback;
    }

    public String getEvent() {
        return event;
    }

    public ChannelCallback getCallback() {
        return callback;
    }
}
