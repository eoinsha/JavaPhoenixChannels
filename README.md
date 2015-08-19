# Phoenix Channel Client for Java and Android

Work in Progress

## Building

Using Gradle 2.2.1 or later:

```shell
gradle build
```

Examples below are used with the [Phoenix Chat Example](https://github.com/chrismccord/phoenix_chat_example)

## Example using Groovy
```groovy
import org.phoenixframework.channels.*
def socket = new Socket('ws://localhost:4000/socket/websocket')
socket.connect()
def chan = socket.chan()
chan.join("rooms:lobby", null)
    .receive("ignore", { -> println "IGNORE"})
    .receive("ok", { envelope -> println "JOINED with $envelope" })
chan.on('new:msg', { -> println "NEW MESSAGE: $envelope"})

```

## Example using Java
```java
import org.phoenixframework.channels.*;

Socket socket;
Channel channel;

socket = new Socket("ws://localhost:4000/socket/websocket");
socket.connect();

channel = socket.chan("rooms:lobby", null);

channel.join()
.receive("ignore", new IMessageCallback() {
    @Override
    public void onMessage(Envelope envelope) {
        System.out.println("IGNORE");
    }
})
.receive("ok", new IMessageCallback() {
    @Override
    public void onMessage(Envelope envelope) {
        System.out.println("JOINED with " + envelope.toString());
    }
});

channel.on("new:msg", new IMessageCallback() {
    @Override
    public void onMessage(Envelope envelope) {
        System.out.println("NEW MESSAGE: " + envelope.toString());
    }
});

channel.onClose(new IMessageCallback() {
    @Override
    public void onMessage(Envelope envelope) {
        System.out.println("CLOSED: " + envelope.toString());
    }
});

channel.onError(new IErrorCallback() {
    @Override
    public void onError(String reason) {
        System.out.println("ERROR: " + reason);
    }
});

//Sending a message. This library uses Jackson for JSON serialization
ObjectNode node = new ObjectNode(JsonNodeFactory.instance)
        .put("user", "my_username")
        .put("body", message);

channel.push("new:msg", node);
```