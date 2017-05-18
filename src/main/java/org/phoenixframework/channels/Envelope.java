package org.phoenixframework.channels;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public class Envelope {
    @JsonProperty
    private String topic;

    @JsonProperty
    private String event;

    @JsonProperty(value = "payload")
    private JsonNode payload;

    @JsonProperty
    private String ref;

    @SuppressWarnings("unused")
    public Envelope() {
    }

    public Envelope(final String topic, final String event, final JsonNode payload, final String ref) {
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

    public JsonNode getPayload() {
        return payload;
    }

    /**
     * Helper to retrieve the value of "ref" from the payload
     *
     * @return The ref string or null if not found
     */
    public String getRef() {
        if (ref != null) return ref;
        final JsonNode refNode = payload.get("ref");
        return refNode != null ? refNode.textValue() : null;
    }

    /**
     * Helper to retrieve the value of "status" from the payload
     *
     * @return The status string or null if not found
     */
    public String getResponseStatus() {
        final JsonNode statusNode = payload.get("status");
        return statusNode == null ? null : statusNode.textValue();
    }

    /**
     * Helper to retrieve the value of "reason" from the payload
     *
     * @return The reason string or null if not found
     */
    public String getReason() {
        final JsonNode reasonNode = payload.get("reason");
        return reasonNode == null ? null : reasonNode.textValue();
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
