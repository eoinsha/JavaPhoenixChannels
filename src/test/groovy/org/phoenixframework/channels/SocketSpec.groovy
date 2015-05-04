package org.phoenixframework.channels

import spock.lang.Specification
import spock.util.concurrent.BlockingVariable

class SocketSpec extends Specification {

    def socket = new Socket("ws://localhost:4000/ws")

    def socketCallback = Mock(SocketCallback)

    def setup() {
        socket.onOpen(socketCallback)
        .onClose(socketCallback)
        .onMessage(socketCallback)
        .onError(socketCallback)
    }

    def cleanup() {
        socket.disconnect()
    }

    def "Socket connects"() {
        when:
        socket.connect()
        then:
        1 * socketCallback.onOpen()
    }

    def "Channel subscribe"() {
        def channel = new BlockingVariable<Channel>()
        def chanCallback = new ChannelCallback(){
            public void onChannel(Channel chan) {
                channel.set(chan)
            }
        }

        when:
        socket.connect()
        socket.join("rooms:lobby").receive("ok", chanCallback)
        then:
        channel.get() != null
        channel.get().getTopic() == "rooms:lobby"
    }
}