package org.phoenixframework.channels;

public interface IMessageCallback {

    /**
     * @param envelope
     */
    public void onMessage(final Envelope envelope);
}
