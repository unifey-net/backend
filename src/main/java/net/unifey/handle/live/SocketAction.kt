package net.unifey.handle.live

import net.unifey.handle.live.objs.SocketSession

/** A socket action. */
interface SocketAction {
    /** When an action receives an invoke. */
    suspend fun SocketSession.receive()
}
