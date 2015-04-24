package org.phoenixframework.channels;

public abstract class PhxCallback {
    public void onMessage(Message message) {}

    public void onChannel(Channel channel) {}
}
