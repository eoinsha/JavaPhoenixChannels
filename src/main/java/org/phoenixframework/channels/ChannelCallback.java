package org.phoenixframework.channels;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ChannelCallback extends PhxCallback {
    private static final Logger LOG = Logger.getLogger(ChannelCallback.class.getName());

    public void onChannel(final Channel channel) {LOG.log(Level.FINE, "onChannel: {0}", channel);}
}
