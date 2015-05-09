# Phoenix Channel Client for Java and Android

Work in Progress

## Building

Using Gradle 2.2.1 or later:

```shell
gradle build
```

## Example Groovy Client
```groovy
import org.phoenixframework.channels.*
def socket = new Socket('ws://localhost:4000/ws')
socket.connect()
def chan = socket.chan()
chan.join("rooms:lobby", null)
    .receive("ignore", { -> println "IGNORE"})
    .receive("ok", { envelope -> println "JOINED with $envelope" })
chan.on('message_feed', { envelope -> println "MESSAGE FEED: $envelope"})
    .on('ping', { -> println "PING" })
    .on('new_msg', { -> println "NEW MESSAGE: $envelope"})

```

## Running the Sample Client
- The following will run the Groovy test client application for Chris McCord's [Phoenix Takes Flight - Chat](https://github.com/chrismccord/phoenix_takes_flight) application
- See [SimpleCliTest.groovy](src/main/groovy/SimpleCliTest.groovy) for the sample client source
```
gradle runGroovyTest
```
