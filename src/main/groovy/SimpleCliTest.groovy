import org.phoenixframework.channels.*

def chanCallback = { envelope ->
        println "MESSAGE(Envelope)[$envelope.topic/$envelope.event]: ${envelope.getPayload().get('body')}"
}

def socket = new Socket("ws://localhost:4000/ws")
socket.onOpen({ -> println "Socket OPEN"})
    .onClose({ -> println "Socket CLOSE"})
    .onError({ reason -> println "Socket ERROR : $reason"})
    .onMessage({envelope -> println "SOCKET MESSAGE: $envelope"})
    .connect()
def chan = socket.chan("rooms:lobby", null)
chan.join()
    .receive("ignore", chanCallback)
    .receive("ok", chanCallback)
chan.on("message_feed", {envelope ->
        println "MESSAGES: "
        envelope.getPayload().get('messages').each{ message -> println "\t${message.get('body')}"}
    })
    .on("ping", new IMessageCallback() {void onMessage(Envelope envelope){println "PING!"}})
    .on("new_msg", chanCallback)

def quit = {System.exit(0)}
def scanner = new Scanner(System.in)
def input = ""
while(input != null) {
    input = scanner.nextLine().trim()
    println "PUSHING $input"
    def payload = new Payload()
    payload.set("body", input)
    chan.push("new_msg", payload)
            .receive("ok", { envelope -> println "ME: ${envelope.getPayload().getResponse().get("body")}"})
            .after(500, { -> "AFTER Timeout"});
}

input = scanner.nextLine().trim()