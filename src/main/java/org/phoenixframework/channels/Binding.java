package org.phoenixframework.channels;

class Binding {
    final String event;
    final IMessageCallback callback;

    public Binding(final String event, final IMessageCallback callback) {
        this.event = event;
        this.callback = callback;
    }

    public String getEvent() {
        return event;
    }

    public IMessageCallback getCallback() {
        return callback;
    }
}
