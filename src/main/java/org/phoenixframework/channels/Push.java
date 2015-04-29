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
    private Payload payload = null;
    private Envelope receivedEnvelope = null;
//  TODO  private List afterHooks = null;
    private Map<String, ChannelCallback> recHooks = new HashMap<String, ChannelCallback>();
    private boolean sent = false;

    Push(final Channel channel, final String event, final Payload payload) {
        this(channel, event, payload, null);
    }

    Push(final Channel channel, final String event, final Payload payload, final Push mergePush) {
        this.channel = channel;
        this.event = event;
        this.payload = payload;
        if(mergePush != null) {
            // TODO - Merge afterHooks ?
            this.recHooks.putAll(mergePush.getRecHooks());
        }
    }

    void send() throws IOException {
        final String ref = channel.getSocket().makeRef();
        final String refEvent = Socket.replyEventName(ref);

        LOG.log(Level.FINE, "Push send, ref={0}", ref);

        this.channel.on(refEvent, new ChannelCallback() {
            @Override
            public void onMessage(final Envelope envelope) {
                Push.this.receivedEnvelope = envelope;
                Push.this.matchReceive(((Payload)receivedEnvelope.getPayload()).getResponseStatus(), envelope, ref);
                Push.this.channel.off(refEvent);
                // TODO Push.this.cancelAfters();
            }
        });

        // TODO this.startAfters();
        this.sent = true;
        final Envelope envelope = new Envelope(this.channel.getTopic(), this.event, this.payload, ref);
        this.channel.getSocket().send(envelope);
    }

    /**
     * @param status
     * @param callback
     *
     * @return This instance's self
     */
    Push receive(final String status, final ChannelCallback callback) {
        if(this.receivedEnvelope != null) {
            final String receivedStatus = this.receivedEnvelope.getPayload().getResponseStatus();
            if(receivedStatus != null && receivedStatus.equals(status)) {
                callback.onMessage(this.receivedEnvelope); // TODO - What is best to provide here. Polymorphic messages.
            }
        }
        this.recHooks.put(status, callback);
        return this;
    }

    private void matchReceive(final String status, final Envelope envelope, final String ref) {
        final ChannelCallback callback = this.recHooks.get(status);
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

    Payload getPayload() {
        return payload;
    }

    Envelope getReceivedEnvelope() {
        return receivedEnvelope;
    }

    Map<String, ChannelCallback> getRecHooks() {
        return recHooks;
    }

    boolean isSent() {
        return sent;
    }
}