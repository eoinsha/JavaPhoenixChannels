package org.phoenixframework.channels;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializableWithType;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;
import java.util.logging.Logger;

public class Payload {
    private static final Logger LOG = Logger.getLogger(PhoenixSocket.class.getName());

    private String topic;

    @Override
    public String toString() {
        return "Payload{" +
                "topic='" + topic + '\'' +
                ", event='" + event + '\'' +
                ", message=" + message +
                '}';
    }

    private String event;
    private Message message;

    public Payload(final String topic, final String event, final Message message) {
        this.topic = topic;
        this.event = event;
        this.message = message;
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

}
