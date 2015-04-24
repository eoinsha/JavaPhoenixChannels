package org.phoenixframework.channels;

public enum ChannelEvent {
    CLOSE("phx_close"),
    ERROR("phx_error"),
    JOIN("phx_join"),
    REPLY("phx_reply"),
    LEAVE("phx_leave");

    final String phxEvent;

    private ChannelEvent(final String phxEvent) {
        this.phxEvent = phxEvent;
    }

    public String getPhxEvent() {
        return phxEvent;
    }
}
