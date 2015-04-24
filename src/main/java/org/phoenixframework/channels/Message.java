package org.phoenixframework.channels;

public class Message {
    private String subject;
    private String body;

    @Override
    public String toString() {
        return "Message{" +
                "subject='" + subject + '\'' +
                ", body='" + body + '\'' +
                '}';
    }

    public Message(final String subject, final String body) {
        this.subject = subject;
        this.body = body;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(final String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(final String body) {
        this.body = body;
    }
}
