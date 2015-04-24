package org.phoenixframework.channels;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Push {
    private static final Logger LOG = Logger.getLogger(Push.class.getName());

    private Channel channel = null;
    private String event = null;
    private Message message = null;
    private Message receivedMessage = null;
//  TODO  private List afterHooks = null;
    private Map<String, PhxCallback> recHooks = new HashMap<String, PhxCallback>();
    private boolean sent = false;

    public Push(final Channel channel, final String event, final Envelope envelope, final Push mergePush) {
        this.channel = channel;
        this.event = event;
        this.message = message;
        if(mergePush != null) {
            // TODO - Merge afterHooks ?
            this.recHooks.putAll(mergePush.getRecHooks());
        }
    }

    public void send() throws IOException {
        final String ref = channel.getSocket().makeRef();
        final String refEvent = PhxSocket.replyEventName(ref);

        LOG.log(Level.FINE, "Push send, ref={0}", ref);

        this.channel.on(refEvent, new PhxCallback() {
            @Override
            public void onMessage(final Message message) {
                Push.this.receivedMessage = message;
                Push.this.matchReceive(message.getResponseStatus(), message, ref);
                Push.this.channel.off(refEvent);
                // TODO Push.this.cancelAfters();
            }
        });

        // TODO this.startAfters();
        this.sent = true;
        final Envelope envelope = new Envelope(this.channel.getTopic(), this.event, this.message, ref);
        this.channel.getSocket().send(envelope);
    }

    /**
     * @param status
     * @param callback
     *
     * @return This instance's self
     */
    public Push receive(final String status, final PhxCallback callback) {
        if(this.receivedMessage != null) {
            final String receivedStatus = this.receivedMessage.getResponseStatus();
            if(receivedStatus != null && receivedStatus.equals(status)) {
                callback.onMessage(this.receivedMessage); // TODO - What is best to provide here. Polymorphic messages.
            }
        }
        this.recHooks.put(status, callback);
        return this;
    }

    private void matchReceive(final String status, final Message response, final String ref) {
        final PhxCallback callback = this.recHooks.get(status);
        if(callback != null) {
            if(this.event.equals(ChannelEvent.JOIN.getPhxEvent())) {
                callback.onChannel(this.channel);
            }
            else {
                callback.onMessage(response);
            }
        }
    }

    Channel getChannel() {
        return channel;
    }

    String getEvent() {
        return event;
    }

    Message getMessage() {
        return message;
    }

    Message getReceivedMessage() {
        return receivedMessage;
    }

    Map<String, PhxCallback> getRecHooks() {
        return recHooks;
    }

    boolean isSent() {
        return sent;
    }
}