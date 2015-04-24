package org.phoenixframework.channels;

import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@JsonIgnoreProperties(ignoreUnknown = false)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class Message {
    private static final Logger LOG = Logger.getLogger(Message.class.getName());

    /**
     * The message's dynamic fields
     */
    private Map<String, Object> fields = new HashMap<String, Object>();

    public Message(){}

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("Message{");
        for(final Map.Entry<String, Object> field : fields.entrySet()) {
            builder.append(field.getKey()).append(':').append(field.getValue());
        }
        return builder.append('}').toString();
    }

    public Message(final String body) {
        setBody(body);
    }

    @JsonAnySetter
    public void setJSONAny(final String name, final Object value) {
        LOG.log(Level.FINE, "setJSONAny({0},{1}", new Object[]{name, value.getClass()});
        this.fields.put(name, value);
    }

    /**
     * Helper for standard 'body' used in requests (.body) and responses (.response.body)
     * @param body
     */
    public void setBody(final String body) {
        fields.put("body", body);
    }

    /**
     * Helper for standard 'status' in response status.
     *
     * @return The status String. If the field is not found and as expected, <code>null</code> is returned.
     */
    public String getResponseStatus() {
        Object status = fields.get("status");
        if(status != null && status instanceof String) {
           return status.toString();
        }
        LOG.log(Level.WARNING, "Unexpected or missing status found in payload of message {0}", this);
        return null;
    }

    /**
     * @return A map of fields in the response element, if present and as expected, otherwise <code>null</code>
     */
    private Map<String, Object> getResponse() {
        Object responseObject = fields.get("response");
        if(!(responseObject instanceof Map)) {
            LOG.log(Level.WARNING, "Unexpected type {0} found for response in payload of message {1}",
                    new Object[]{responseObject.getClass().getName(), this});
            return null;
        }
        return (Map<String, Object>)responseObject;
    }
}
