import org.phoenixframework.channels.ChannelCallback
import org.phoenixframework.channels.Socket

import java.util.logging.Level
import java.util.logging.Logger;

Logger.getLogger("GROOVY").log(Level.INFO, "GROOVY START")
def socket = new Socket('ws://localhost:4000/ws')
socket.connect()
def chan = socket.join("rooms:lobby", null)
    .receive("ignore", new ChannelCallback(){})
    .receive("ok", new ChannelCallback(){})
chan.on('message_feed', new ChannelCallback(){})
    .on('ping', new ChannelCallback(){})
    .on('new_msg', new ChannelCallback(){})




