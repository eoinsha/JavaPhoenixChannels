package org.phoenixframework.channels;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ChannelCallback extends PhxCallback {
    private static final Logger LOG = Logger.getLogger(ChannelCallback.class.getName());

    public void onChannel(final Channel channel) {LOG.log(Level.FINE, "onChannel: {0}", channel);}

    public void onMessage(final String topic, final String event, final Payload payload) {LOG.log(Level.FINE, "onMessage: {0}, {1}, {2}", new Object[]{topic, event, payload});}
}
