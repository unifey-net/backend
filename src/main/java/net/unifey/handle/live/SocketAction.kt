package net.unifey.handle.live

import io.ktor.http.cio.websocket.*
import net.unifey.auth.tokens.Token
import org.json.JSONObject

/**
 * A socket action.
 */
interface SocketAction {
    /**
     * When a action receives an invoke.
     */
    suspend fun WebSocketSession.receive(auth: Token, data: JSONObject): Boolean
}