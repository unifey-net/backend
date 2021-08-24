package net.unifey.handle.live

import dev.shog.lib.util.jsonObjectOf
import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import net.unifey.FRONTEND_EXPECT
import net.unifey.VERSION
import net.unifey.auth.tokens.Token
import net.unifey.auth.tokens.TokenManager
import net.unifey.handle.socket.WebSocket.authenticateMessage
import net.unifey.handle.socket.WebSocket.customTypeMessage
import net.unifey.handle.socket.WebSocket.errorMessage
import net.unifey.handle.socket.WebSocket.successMessage
import net.unifey.response.Response
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis

/**
 * The logger for the socket.
 */
val socketLogger = LoggerFactory.getLogger("SOCKET")

/**
 * This is the websocket implementation as well as the REST implementation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun Routing.liveSocket() {
    route("/live") {
        get("/count") {
            call.respond(Response(Live.getOnlineUsers().size))
        }

        webSocket {
            var token: Token? = null

            launch {
                while (!Live.CHANNEL.isClosedForReceive) {
                    val (type, user, data) = Live.CHANNEL.receive()

                    if (Live.getOnlineUsers().contains(user) && token?.owner == user) {
                        socketLogger.info("SEND $user: LIVE $type ($data)")
                        customTypeMessage(type, data)
                    }
                }
            }

            customTypeMessage("init", jsonObjectOf(
                "version" to "Unifey Backend $VERSION",
                "frontend" to FRONTEND_EXPECT
            ))

            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val data = String(frame.data)

                        if (data.startsWith("bearer")) {
                            val split = data.split(" ")

                            if (split[0].equals("bearer", true)) {
                                val tokenObj = TokenManager.getToken(split[1])

                                if (tokenObj == null) {
                                    errorMessage("Invalid token.")
                                } else {
                                    token = tokenObj

                                    socketLogger.info("AUTH ${tokenObj.owner}: SUCCESS")

                                    Live.userOnline(tokenObj.owner)
                                    authenticateMessage()
                                }
                            } else
                                errorMessage("Invalid authentication method.")
                        } else if (token != null) {
                            try {
                                handleIncoming(token, data)
                            } catch (ex: SocketError) {
                                socketLogger.info("CLOSE: ${ex.message}")
                                close(ex.reason)
                            }
                        } else {
                            errorMessage("Not authenticated.")
                        }
                    }

                    else -> errorMessage("Unexpected frame.")
                }
            }

            if (token != null)
                Live.userOffline(token.owner)
        }
    }
}

/**
 * Handle an incoming websocket action for [user] with [data].
 *
 * @throws SocketError If JSON is incorrect.
 */
@Throws(SocketError::class)
private suspend fun WebSocketSession.handleIncoming(user: Token, data: String) {
    val json = try {
        JSONObject(data)
    } catch (ex: Exception) {
        throw SocketError(400, "Invalid data type, expects JSON.")
    }

    if (!json.has("action") && json["action"] is String)
        throw SocketError(400, "JSON doesn't contain \"action\" parameter.")

    val page = findPage(json.getString("action"))

    if (page != null)
        page.run {
            var success = false
            val time = measureTimeMillis { success = receive(user, json) }

            socketLogger.info("${json.getString("action")} - ${user.owner}: ${if (success) "OK" else "NOT OK"} (took ${time}ms)")
        }
    else {
        errorMessage("That page could not be found.")
    }
}

private fun findPage(action: String): SocketAction? {
    SocketActionHandler.socketActions.forEach { (name, page) ->
        if (action.equals(name, true))
            return page
    }

    return null
}