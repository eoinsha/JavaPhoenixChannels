package org.phoenixframework.channels;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializableWithType;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;

public class Payload {
    private String channel;
    private String topic;
    private String event;
    private Message message;

    public Payload(final String channel, final String topic, final String event, final Message message) {
        this.channel = channel;
        this.topic = topic;
        this.event = event;
        this.message = message;
    }

    public String getChannel() {
        return channel;
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
