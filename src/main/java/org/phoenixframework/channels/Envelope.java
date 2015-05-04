package org.phoenixframework.channels;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.logging.Logger;

public class Envelope {
    private static final Logger LOG = Logger.getLogger(Socket.class.getName());

    @JsonProperty
    private String topic;

    @JsonProperty
    private String event;

    @JsonProperty(value="payload")
    private Payload payload;

    @JsonProperty
    private String ref;

    public Envelope() {}

    public Envelope(final String topic, final String event, final Payload payload, final String ref) {
        this.topic = topic;
        this.event = event;
        this.payload = payload;
        this.ref = ref;
    }

    public String getTopic() {
        return topic;
    }

    public String getEvent() {
        return event;
    }

    public Payload getPayload() {
        return payload;
    }

    public String getRef() {
        if(ref != null) return ref;
        return payload != null ? (String)payload.get("ref") : null;
    }

    @Override
    public String toString() {
        return "Envelope{" +
                "topic='" + topic + '\'' +
                ", event='" + event + '\'' +
                ", payload=" + payload +
                '}';
    }
}
