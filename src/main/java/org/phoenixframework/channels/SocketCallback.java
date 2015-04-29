package org.phoenixframework.channels;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class SocketCallback extends PhxCallback {
    private static final Logger LOG = Logger.getLogger(SocketCallback.class.getName());

    public void onOpen() {LOG.log(Level.FINE, "onOpen");}

    public void onClose() {LOG.log(Level.FINE, "onChannel");}
}
