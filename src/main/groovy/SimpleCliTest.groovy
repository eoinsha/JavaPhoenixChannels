import org.phoenixframework.channels.*

def chanCallback = new ChannelCallback() {
    void onChannel(Channel channel) { println "onChannel: $channel" }
    void onMessage(String topic, String event, Payload payload) { println "onMessage($topic, $event, $payload)" }
    void onError(String reason) { println "onError($reason)" }
    void onMessage(Envelope envelope) { println "onMessage(envelope=$envelope)"  }
}
def socketCallback = new SocketCallback() {
    void onOpen() { println "Socket OPEN" }
    void onClose() { println "Socket CLOSE" }
    void onError(String reason) { println "Socket ERROR: $reason" }
    void onMessage(Envelope envelope) {println "Socket MESSAGE: $envelope"}
}

def socket = new Socket("ws://localhost:4000/ws")
socket.onOpen(socketCallback)
    .onClose(socketCallback)
    .onError(socketCallback)
    .onMessage(socketCallback)
    .connect()
def chan = socket.join("rooms:lobby", null)
    .receive("ignore", chanCallback)
    .receive("ok", chanCallback)
chan.on("message_feed", chanCallback)
    .on("ping", chanCallback)
    .on("new_msg", chanCallback)




