package org.phoenixframework.channels;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

class Push {
    private static final Logger LOG = Logger.getLogger(Push.class.getName());

    private Channel channel = null;
    private String event = null;
    private Message message = null;
    private Envelope receivedEnvelope = null;
//  TODO  private List afterHooks = null;
    private Map<String, PhxCallback> recHooks = new HashMap<String, PhxCallback>();
    private boolean sent = false;

    Push(final Channel channel, final String event, final Message message) {
        this(channel, event, message, null);
    }

    Push(final Channel channel, final String event, final Message message, final Push mergePush) {
        this.channel = channel;
        this.event = event;
        this.message = message;
        if(mergePush != null) {
            // TODO - Merge afterHooks ?
            this.recHooks.putAll(mergePush.getRecHooks());
        }
    }

    void send() throws IOException {
        final String ref = channel.getSocket().makeRef();
        final String refEvent = PhxSocket.replyEventName(ref);

        LOG.log(Level.FINE, "Push send, ref={0}", ref);

        this.channel.on(refEvent, new PhxCallback() {
            @Override
            public void onMessage(final Envelope envelope) {
                Push.this.receivedEnvelope = envelope;
                Push.this.matchReceive(receivedEnvelope.getMessage().getResponseStatus(), envelope, ref);
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
    Push receive(final String status, final PhxCallback callback) {
        if(this.receivedEnvelope != null) {
            final String receivedStatus = this.receivedEnvelope.getMessage().getResponseStatus();
            if(receivedStatus != null && receivedStatus.equals(status)) {
                callback.onMessage(this.receivedEnvelope); // TODO - What is best to provide here. Polymorphic messages.
            }
        }
        this.recHooks.put(status, callback);
        return this;
    }

    private void matchReceive(final String status, final Envelope envelope, final String ref) {
        final PhxCallback callback = this.recHooks.get(status);
        if(callback != null) {
            if(this.event.equals(ChannelEvent.JOIN.getPhxEvent())) {
                callback.onChannel(this.channel);
            }
            else {
                callback.onMessage(envelope);
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

    Envelope getReceivedEnvelope() {
        return receivedEnvelope;
    }

    Map<String, PhxCallback> getRecHooks() {
        return recHooks;
    }

    boolean isSent() {
        return sent;
    }
}