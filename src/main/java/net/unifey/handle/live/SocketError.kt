package net.unifey.handle.live

import io.ktor.http.cio.websocket.*

/**
 * A socket error with [code] and [msg]. Thrown for all socket errors.
 */
class SocketError(code: Short, msg: String): Throwable() {
    val reason = CloseReason(code, msg)
}