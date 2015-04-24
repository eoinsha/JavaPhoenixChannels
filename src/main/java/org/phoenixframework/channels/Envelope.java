package org.phoenixframework.channels;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.logging.Logger;

public class Envelope {
    private static final Logger LOG = Logger.getLogger(PhxSocket.class.getName());

    @JsonProperty
    private String topic;

    @JsonProperty
    private String event;

    @JsonProperty(value="payload")
    private Message message;

    @JsonProperty
    private String ref;

    public Envelope() {}

    public Envelope(final String topic, final String event, final Message message, final String ref) {
        this.topic = topic;
        this.event = event;
        this.message = message;
        this.ref = ref;
    }

    public String getTopic() {
        return topic;
    }

    public String getEvent() {
        return event;
    }

    public Message getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "Envelope{" +
                "topic='" + topic + '\'' +
                ", event='" + event + '\'' +
                ", message=" + message +
                '}';
    }
}
