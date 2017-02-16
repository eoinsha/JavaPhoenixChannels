# Phoenix Channel Client for Java and Android

JavaPhoenixChannels is a Java and Android client library for the Channels API in the [Phoenix Framework](http://www.phoenixframework.org/). Its primary purpose is to ease development of real time messaging apps for Android using an Elixir/Phoenix backend. For more about the Elixir language and the massively scalable and reliable systems you can build with Phoenix, see http://elixir-lang.org and http://www.phoenixframework.org.

## Including the Library

- Add `http://dl.bintray.com/eoinsha/java-phoenix-channels` as a Maven repository
- Add JavaPhoenixChannels as an app dependency:
```
dependencies {
  ...
  compile('com.github.eoinsha:JavaPhoenixChannels:0.2') {
      exclude module: 'groovy-all'
  }
```
- For Maven or Gradle-specific instructions detailing how this is done, see the "SET ME UP" section on https://bintray.com/javaphoenixchannels/java-phoenix-channels/JavaPhoenixChannels and choose "Resolving artifacts..."

# Examples

## Example Android App

For a full sample Android chat app, check out the repository at https://github.com/eoinsha/PhoenixChatAndroid

The quick examples below are used with the [Phoenix Chat Example](https://github.com/chrismccord/phoenix_chat_example)

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

channel.on(ChannelEvent.CLOSE.getPhxEvent(), new IMessageCallback() {
    @Override
    public void onMessage(Envelope envelope) {
        System.out.println("CLOSED: " + envelope.toString());
    }
});

channel.on(ChannelEvent.ERROR.getPhxEvent(), new IMessageCallback() {
    @Override
    public void onMessage(Envelope envelope) {
        System.out.println("ERROR: " + envelope.toString());
    }
});

//Sending a message. This library uses Jackson for JSON serialization
ObjectNode node = new ObjectNode(JsonNodeFactory.instance)
        .put("user", "my_username")
        .put("body", message);

channel.push("new:msg", node);
```

# Contributing

To contribute, see the [contribution guidelines and instructions](./CONTRIBUTING.md).
