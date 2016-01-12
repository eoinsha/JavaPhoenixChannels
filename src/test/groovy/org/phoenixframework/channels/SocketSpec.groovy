package org.phoenixframework.channels

import spock.lang.Specification
import spock.util.concurrent.BlockingVariable

class SocketSpec extends Specification {

    def socket = new Socket("ws://localhost:4000/socket/websocket")

    def socketOpenCallback = Mock(ISocketOpenCallback)
    def socketCloseCallback = Mock(ISocketCloseCallback)
    def socketMessageCallback= Mock(IMessageCallback)
    def socketErrorCallback = Mock(IErrorCallback)

    def setup() {
        socket.onOpen(socketOpenCallback)
        .onClose(socketCloseCallback)
        .onMessage(socketMessageCallback)
        .onError(socketErrorCallback)
    }

    def cleanup() {
        socket.disconnect()
    }

    def "Socket connects"() {
        when:
        socket.connect()
        then:
        1 * socketOpenCallback.onOpen()
    }

    def "Channel subscribe"() {
        def envelope = new BlockingVariable<Envelope>()
        def callback = new IMessageCallback() {
            @Override
            void onMessage(Envelope e) {
                envelope.set(e)
            }
        }

        when:
        socket.connect()
        socket.chan("rooms:lobby", null).join().receive("ok", callback)
        then:
        envelope.get() != null
        envelope.get().getTopic() == "rooms:lobby"
    }
}