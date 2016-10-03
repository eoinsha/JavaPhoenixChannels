import org.phoenixframework.channels.Channel;
import org.phoenixframework.channels.Envelope;
import org.phoenixframework.channels.IErrorCallback;
import org.phoenixframework.channels.IMessageCallback;
import org.phoenixframework.channels.ISocketCloseCallback;
import org.phoenixframework.channels.ISocketOpenCallback;
import org.phoenixframework.channels.Socket;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class ManTest {

  private static Socket socket = null;

  public static void main(final String... args) throws Exception {
    socket = new Socket("ws://phxchat.codespyre.com/socket/websocket");

    socket.onClose(new ISocketCloseCallback() {
      @Override
      public void onClose() {
        log("Socket.onClose()", socket);
      }
    });

    socket.onError(new IErrorCallback() {
      @Override
      public void onError(String reason) {
        log("Socket.onError()", reason, socket);
      }
    });

    socket.onOpen(new ISocketOpenCallback() {
      @Override
      public void onOpen() {
        log("Socket.onOpen()", socket);
        try {
          joinRoomLobby();
        } catch (final Exception e) {
          e.printStackTrace();
        }
      }
    });

    socket.onMessage(new IMessageCallback() {
      @Override
      public void onMessage(Envelope envelope) {
        log("Socket.onMessage()", socket, envelope);
      }
    });

    socket.connect();

    new Timer().schedule(new TimerTask() {
      @Override
      public void run() {
        try {
          log("DISCONNECTING");
          socket.disconnect();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }, 15000L);
  }

  final static void joinRoomLobby() throws Exception {
    final Channel channel = socket.chan("rooms:lobby", null);
    channel.join().
        receive("ignore", new IMessageCallback() {
          @Override
          public void onMessage(Envelope envelope) {
            log("onMessage", "rooms:lobby", "ignore", envelope);
          }
        }).
        receive("ok", new IMessageCallback() {
          @Override
          public void onMessage(Envelope envelope) {
            log("onMessage", "rooms:lobby", "ok", envelope);
            log("LEAVING", "rooms:lobby");
            try {
              channel.leave();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        });

    channel.on("new:msg", new IMessageCallback() {
      @Override
      public void onMessage(Envelope envelope) {
        log("onMessage", "rooms:lobby", "new:msg", envelope);
      }
    });
    channel.onClose(new IMessageCallback() {
      @Override
      public void onMessage(Envelope envelope) {
        log("onClose", "rooms:lobby", envelope);
      }
    });
    channel.onError(new IErrorCallback() {
      @Override
      public void onError(String reason) {
        log("onError", "rooms:lobby", reason);
      }
    });
  }

  final static void log (final Object... args) {
    final StringBuilder msgBuilder = new StringBuilder();
    for (final Object a : args) {
      msgBuilder.append(a).append(" | ");
    }
    System.out.println(msgBuilder);
  }
}
