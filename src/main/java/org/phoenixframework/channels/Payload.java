package org.phoenixframework.channels;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.JsonSerializableWithType;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;
import java.util.logging.Logger;

public class Payload {
    private static final Logger LOG = Logger.getLogger(PhoenixSocket.class.getName());

    @JsonProperty
    private String topic;

    @JsonProperty
    private String event;

    @JsonProperty(value="payload")
    private Message message;

    @JsonProperty
    private String ref;

    public Payload() {}

    public Payload(final String topic, final String event, final Message message, final String ref) {
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
        return "Payload{" +
                "topic='" + topic + '\'' +
                ", event='" + event + '\'' +
                ", message=" + message +
                '}';
    }
}
