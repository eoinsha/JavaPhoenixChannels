import org.phoenixframework.channels.*

import java.util.logging.Level
import java.util.logging.Logger;

Logger.getLogger("GROOVY").log(Level.INFO, "GROOVY START")
def socket = new PhxSocket('ws://localhost:4000/ws')
socket.connect()
socket.join("rooms:lobby", null)



