package org.phoenixframework.channels;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Push {

    private static final Logger log = LoggerFactory.getLogger(Push.class);

    private class TimeoutHook {

        private ITimeoutCallback callback;

        private final long ms;

        private TimerTask timerTask;

        public TimeoutHook(final long ms) {
            this.ms = ms;
        }

        public ITimeoutCallback getCallback() {
            return callback;
        }

        public long getMs() {
            return ms;
        }

        public TimerTask getTimerTask() {
            return timerTask;
        }

        public boolean hasCallback() {
            return this.callback != null;
        }

        public void setCallback(final ITimeoutCallback callback) {
            this.callback = callback;
        }

        public void setTimerTask(final TimerTask timerTask) {
            this.timerTask = timerTask;
        }
    }

    private Channel channel = null;

    private String event = null;

    private JsonNode payload = null;

    private final Map<String, List<IMessageCallback>> recHooks = new HashMap<>();

    private Envelope receivedEnvelope = null;

    private String refEvent = null;

    private boolean sent = false;

    private final TimeoutHook timeoutHook;

    Push(final Channel channel, final String event, final JsonNode payload, final long timeout) {
        this.channel = channel;
        this.event = event;
        this.payload = payload;
        this.timeoutHook = new TimeoutHook(timeout);
    }

    /**
     * Registers for notifications on status messages
     *
     * @param status   The message status to register callbacks on
     * @param callback The callback handler
     * @return This instance's self
     */
    public Push receive(final String status, final IMessageCallback callback) {
        if (this.receivedEnvelope != null) {
            final String receivedStatus = this.receivedEnvelope.getResponseStatus();
            if (receivedStatus != null && receivedStatus.equals(status)) {
                callback.onMessage(this.receivedEnvelope);
            }
        }
        synchronized (recHooks) {
            List<IMessageCallback> statusHooks = this.recHooks.get(status);
            if (statusHooks == null) {
                statusHooks = new ArrayList<>();
                this.recHooks.put(status, statusHooks);
            }
            statusHooks.add(callback);
        }

        return this;
    }

    /**
     * Registers for notification of message response timeout
     *
     * @param callback The callback handler called when timeout is reached
     * @return This instance's self
     */
    public Push timeout(final ITimeoutCallback callback) {
        if (this.timeoutHook.hasCallback()) {
            throw new IllegalStateException("Only a single after hook can be applied to a Push");
        }

        this.timeoutHook.setCallback(callback);

        return this;
    }

    Channel getChannel() {
        return channel;
    }

    String getEvent() {
        return event;
    }

    JsonNode getPayload() {
        return payload;
    }

    Map<String, List<IMessageCallback>> getRecHooks() {
        return recHooks;
    }

    Envelope getReceivedEnvelope() {
        return receivedEnvelope;
    }

    boolean isSent() {
        return sent;
    }

    void send() throws IOException {
        final String ref = channel.getSocket().makeRef();
        log.trace("Push send, ref={}", ref);

        this.refEvent = Socket.replyEventName(ref);
        this.receivedEnvelope = null;

        this.channel.on(this.refEvent, new IMessageCallback() {
            @Override
            public void onMessage(final Envelope envelope) {
                Push.this.receivedEnvelope = envelope;
                Push.this.matchReceive(receivedEnvelope.getResponseStatus(), envelope);
                Push.this.cancelRefEvent();
                Push.this.cancelTimeout();
            }
        });

        this.startTimeout();
        this.sent = true;
        final Envelope envelope = new Envelope(this.channel.getTopic(), this.event, this.payload, ref);
        this.channel.getSocket().push(envelope);
    }

    private void cancelRefEvent() {
        this.channel.off(this.refEvent);
    }

    private void cancelTimeout() {
        this.timeoutHook.getTimerTask().cancel();
        this.timeoutHook.setTimerTask(null);
    }

    private TimerTask createTimerTask() {
        final Runnable callback = new Runnable() {
            @Override
            public void run() {
                Push.this.cancelRefEvent();
                if (Push.this.timeoutHook.hasCallback()) {
                    Push.this.timeoutHook.getCallback().onTimeout();
                }
            }
        };

        return new TimerTask() {
            @Override
            public void run() {
                callback.run();
            }
        };
    }

    private void matchReceive(final String status, final Envelope envelope) {
        synchronized (recHooks) {
            final List<IMessageCallback> statusCallbacks = this.recHooks.get(status);
            if (statusCallbacks != null) {
                for (final IMessageCallback callback : statusCallbacks) {
                    callback.onMessage(envelope);
                }
            }
        }
    }

    private void startTimeout() {
        this.timeoutHook.setTimerTask(createTimerTask());
        this.channel.scheduleTask(this.timeoutHook.getTimerTask(), this.timeoutHook.getMs());
    }
}