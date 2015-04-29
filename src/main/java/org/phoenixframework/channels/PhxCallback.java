package org.phoenixframework.channels;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class PhxCallback {
    private static final Logger LOG = Logger.getLogger(PhxCallback.class.getName());

    public void onError(final String reason) {LOG.log(Level.SEVERE, "onError: {0}", reason);}

    /**
     * TODO - Seems like this need only be in ChannelCallback
     * @param envelope
     */
    public void onMessage(final Envelope envelope) {LOG.log(Level.FINE, "onMessage: {0}", envelope);}
}
