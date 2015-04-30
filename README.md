# Phoenix Channel Client for Java and Android

Work in Progress

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