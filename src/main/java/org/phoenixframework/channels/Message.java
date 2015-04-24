package org.phoenixframework.channels;

import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@JsonIgnoreProperties(ignoreUnknown = false)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class Message {
    private static final Logger LOG = Logger.getLogger(Message.class.getName());

    /**
     * Payload of the message
     */
    private Object body;

    public Message(){}

    @Override
    public String toString() {
        return "Message{" +
                "body='" + body + '\'' +
                '}';
    }

    public Message(final Object body) {
        this.body = body;
    }

    public Object getBody() {
        return body;
    }

    @JsonAnySetter
    public void setJSONAny(String name, Object value) {
        LOG.log(Level.FINE, "setJSONAny({0},{1}", new Object[]{name, value.getClass()});
        this.body = value;
    }

    public void setBody(final String body) {
        this.body = body;
    }
}
