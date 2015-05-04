package org.phoenixframework.channels;

import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO - Implement field setters
 */
@JsonIgnoreProperties(ignoreUnknown = false)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class Payload {
    private static final Logger LOG = Logger.getLogger(Payload.class.getName());

    /**
     * The message's dynamic fields
     */
    private Map<String, Object> fields = new HashMap<>();

    public Payload(){}

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("Payload{");
        for(final Map.Entry<String, Object> field : fields.entrySet()) {
            builder.append(field.getKey()).append(':').append(field.getValue()).append(", ");
        }
        return builder.append('}').toString();
    }


    @JsonAnySetter
    public void set(final String name, final Object value) {
        LOG.log(Level.FINE, "set({0},{1}", new Object[]{name, value.getClass()});
        this.fields.put(name, value);
    }


    /**
     * @param fieldName
     * @return The value for the dynamic field in the payload
     */
    public Object get(final String fieldName) {
        return fields.get(fieldName);
    }


    @JsonAnyGetter
    public Map<String, Object> getAll() { return fields; }

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
        return null;
    }

    /**
     * @param response A map of fields in the repsonse element with a hierarchical substructure
     */
    public void setResponse(final Map<String, Object> response) {
        fields.put("response", response);
    }

    /**
     * @return A map of fields in the response element, if present and as expected, otherwise <code>null</code>
     */
    public Map<String, Object> getResponse() {
        Object responseObject = fields.get("response");
        if(responseObject == null) {
            return null;
        }
        if(!(responseObject instanceof Map)) {
            LOG.log(Level.WARNING, "Unexpected type {0} found for response in payload of message {1}",
                    new Object[]{responseObject.getClass().getName(), this});
            return null;
        }
        return (Map<String, Object>)responseObject;
    }
}
