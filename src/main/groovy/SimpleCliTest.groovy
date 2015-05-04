import org.phoenixframework.channels.*

def chanCallback = new ChannelCallback() {
    void onChannel(Channel channel) { println "onChannel: $channel.topic" }
    void onError(String reason) { println "ERROR($reason)" }
    void onMessage(Envelope envelope) {
        println "MESSAGE(Envelope)[$envelope.topic/$envelope.event]: ${envelope.getPayload().get('body')}"
    }
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
    void onMessage(Envelope envelope){
        println "MESSAGES: "
        envelope.getPayload().get('messages').each{ message -> println "\t${message.get('body')}"}
    }})
    .on("ping", new ChannelCallback() {void onMessage(Envelope envelope){println "PING!"}})
    .on("new_msg", chanCallback)

def quit = {System.exit(0)}
def scanner = new Scanner(System.in)
def input = ""
while(input != null) {
    input = scanner.nextLine().trim()
    println "PUSHING $input"
    def payload = new Payload()
    payload.set("body", input)
    chan.push("new_msg", payload).receive("ok", new ChannelCallback(){
        void onMessage(Envelope envelope){
            println "ME: ${envelope.getPayload().getResponse().get("body")}"
    }});
}

input = scanner.nextLine().trim()



