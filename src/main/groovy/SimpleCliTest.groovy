import org.phoenixframework.channels.*

def chanCallback = new ChannelCallback() {
    void onChannel(Channel channel) { println "onChannel: $channel.topic" }
    void onMessage(String topic, String event, Payload payload) { println "MESSAGE[$topic/$event]: $payload.body" }
    void onError(String reason) { println "ERROR($reason)" }
    void onMessage(Envelope envelope) { println "MESSAGE(Envelope)[$envelope.topic/$envelope.event]: $envelope.payload.body"  }
}
def socketCallback = new SocketCallback() {
    void onOpen() { println "Socket OPEN" }
    void onClose() { println "Socket CLOSE" }
    void onError(String reason) { println "Socket ERROR: $reason" }
    void onMessage(Envelope envelope) {}
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
chan.on("message_feed", new ChannelCallback(){
    void onMessage(String topic, String event, Payload payload){
        println "MESSAGES: "
        payload.get('messages').each{ message -> println "\t${message.get('body')}"}
    }})
    .on("ping", new ChannelCallback() {void onMessage(String topic, String event, Payload payload){println "PING!"}})
    .on("new_msg", chanCallback)

def quit = {System.exit(0)}
def scanner = new Scanner(System.in)
def input = ""
while(input != null) {
    chan.push("new_msg", new Payload(input)).receive("ok", new ChannelCallback(){
        void onMessage(String topic, String event, Payload payload){
            println "ME: $payload.body"
    }});
}

input = scanner.nextLine().trim()



