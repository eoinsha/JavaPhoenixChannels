package org.phoenixframework.channels;

public interface IMessageCallback {

    /**
     * @param envelope The envelope containing the message payload and properties
     */
    public void onMessage(final Envelope envelope);
}
