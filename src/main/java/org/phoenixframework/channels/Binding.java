package org.phoenixframework.channels;

public class Binding {
    final String event;
    final PhxCallback callback;

    public Binding(final String event, final PhxCallback callback) {
        this.event = event;
        this.callback = callback;
    }

    public String getEvent() {
        return event;
    }

    public PhxCallback getCallback() {
        return callback;
    }
}
