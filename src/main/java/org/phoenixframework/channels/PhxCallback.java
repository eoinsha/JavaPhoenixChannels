package org.phoenixframework.channels;

public abstract class PhxCallback {
    public void onMessage(Envelope message) {}

    public void onChannel(Channel channel) {}

    public void onError(String reason) {}

    public void onOpen() {}

    public void onClose() {}
}
