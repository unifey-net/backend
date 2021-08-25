package net.unifey.handle.live

import io.ktor.http.cio.websocket.*

class SocketError(code: Short, msg: String): Throwable() {
    val reason = CloseReason(code, msg)
}