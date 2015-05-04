package org.phoenixframework.channels;

public enum ChannelEvent {
    CLOSE("phx_close"),
    ERROR("phx_error"),
    JOIN("phx_join"),
    REPLY("phx_reply"),
    LEAVE("phx_leave");

    private final String phxEvent;

    private ChannelEvent(final String phxEvent) {
        this.phxEvent = phxEvent;
    }

    public String getPhxEvent() {
        return phxEvent;
    }

    public static ChannelEvent getEvent(final String phxEvent) {
        for(final ChannelEvent ev : values()) {
            if(ev.getPhxEvent().equals(phxEvent)) {
                return ev;
            }
        }
        return null;
    }
}
