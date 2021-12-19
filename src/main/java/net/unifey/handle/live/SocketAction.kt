package net.unifey.handle.live

import io.ktor.http.cio.websocket.*

/** A socket action. */
interface SocketAction {
    /** When an action receives an invoke. */
    suspend fun SocketSession.receive(): Boolean
}
