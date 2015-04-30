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
def chan = socket.join("rooms:lobby", null)
    .receive("ignore", new ChannelCallback(){})
    .receive("ok", new ChannelCallback(){})
chan.on('message_feed', new ChannelCallback(){})
    .on('ping', new ChannelCallback(){})
    .on('new_msg', new ChannelCallback(){})

```

## Running the Sample Client
- The following will run the Groovy test client application for Chris McCord's [Phoenix Takes Flight - Chat](https://github.com/chrismccord/phoenix_takes_flight) application
 - See [SimpleCliTest.groovy](src/main/groovy/SimpleCliTest.groovy) for the sample client source
```
gradle runGroovyTest
```
