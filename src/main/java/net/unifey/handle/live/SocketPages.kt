package net.unifey.handle.live

import dev.ajkneisl.lib.util.jsonObjectOf
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.unifey.Unifey
import net.unifey.auth.isAuthenticated
import net.unifey.auth.tokens.Token
import net.unifey.auth.tokens.TokenManager
import net.unifey.handle.live.WebSocket.authenticateMessage
import net.unifey.handle.live.WebSocket.customTypeMessage
import net.unifey.handle.live.WebSocket.errorMessage
import net.unifey.handle.live.objs.SocketSession
import net.unifey.handle.live.objs.SocketType
import net.unifey.response.Response
import org.json.JSONObject
import org.slf4j.LoggerFactory

/** The logger for the socket. */
val socketLogger = LoggerFactory.getLogger(object {}.javaClass.enclosingClass)

/** This is the websocket implementation as well as the REST implementation. */
@OptIn(ExperimentalCoroutinesApi::class)
fun Route.liveSocket() {
    route("/manage-live") {
        get("/view") {
            @Serializable
            data class SocketSessionResponse(
                val owner: Long,
                val connected: Boolean,
                val sessionLength: Long
            )
            val token = call.isAuthenticated()

            val session = Live.getOnlineUsers()[token.owner]
            if (session != null) {
                call.respond(
                    SocketSessionResponse(
                        token.owner,
                        true,
                        System.currentTimeMillis() - session.connectedAt
                    )
                )
            } else {
                call.respond(Response("Currently not connected anywhere else."))
            }
        }

        delete("/logout") {
            val token = call.isAuthenticated()

            if (Live.getOnlineUsers().containsKey(token.owner)) {
                Live.sendUpdate(Live.LiveObject("DISCONNECT", token.owner, ""))
                call.respond(HttpStatusCode.OK, Response("Disconnected other account."))
            } else {
                call.respond(HttpStatusCode.BadRequest, Response("No one is connected!"))
            }
        }
    }

    route("/live") {
        get("/count") { call.respond(Response(Live.getOnlineUsers().size)) }

        webSocket {
            var token: Token? = null
            val channel = Channel<Live.LiveObject>()

            launch {
                while (!channel.isClosedForReceive) {
                    val (type, user, data) = channel.receive()

                    if (type.equals("DISCONNECT", true)) {
                        customTypeMessage("DISCONNECT", JSONObject())
                        close(
                            CloseReason(CloseReason.Codes.GOING_AWAY, "Connected somewhere else.")
                        )
                    } else {
                        socketLogger.info("SEND $user: LIVE $type ($data)")
                        customTypeMessage(type, data)
                    }
                }
            }

            customTypeMessage(
                "init",
                JSONObject()
                    .put("frontend", Unifey.FRONTEND_EXPECT)
                    .put("version", "Unifey Backend ${Unifey.VERSION}")
            )

            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val data = String(frame.data)

                        when {
                            data.startsWith("bearer") -> {
                                val split = data.split(" ")

                                if (split[0].equals("bearer", true)) {
                                    val tokenObj = TokenManager.getToken(split[1])

                                    if (tokenObj == null) {
                                        errorMessage("Invalid token.")
                                    } else {
                                        if (TokenManager.isTokenExpired(tokenObj)) {
                                            socketLogger.info(
                                                "SOCK AUTH: Failed due to expired token."
                                            )
                                            customTypeMessage(
                                                "TOKEN_EXPIRED",
                                                "Your token has expired!"
                                            )
                                            close(CloseReason(4011, "Your token was expired!"))
                                        } else {
                                            token = tokenObj

                                            if (Live.getOnlineUsers().keys.contains(tokenObj.owner)
                                            ) {
                                                socketLogger.info(
                                                    "AUTH ${tokenObj.owner}: FAILED, LOGGED IN SOMEWHERE ELSE"
                                                )

                                                close(
                                                    CloseReason(
                                                        CloseReason.Codes.VIOLATED_POLICY,
                                                        "You're already logged in somewhere else!"
                                                    )
                                                )
                                            } else {
                                                socketLogger.info("AUTH ${tokenObj.owner}: SUCCESS")

                                                Live.userOnline(tokenObj.owner, channel)
                                                authenticateMessage()
                                            }
                                        }
                                    }
                                }
                            }
                            data.startsWith("ping") -> {
                                customTypeMessage("PONG", jsonObjectOf())
                            }
                            token != null -> {
                                try {
                                    handleIncoming(token, data)
                                } catch (ex: SocketError) {
                                    socketLogger.info("CLOSE: ${ex.message}, ${ex.reason}")
                                    close(ex.reason)
                                }
                            }
                            else -> errorMessage("Not authenticated.")
                        }
                    }
                    else -> errorMessage("Unexpected frame.")
                }
            }

            if (token != null) Live.userOffline(token.owner)
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
    val json =
        try {
            JSONObject(data)
        } catch (ex: Exception) {
            throw SocketError(400, "Invalid data type, expects JSON.")
        }

    if (!json.has("action") || json["action"] !is String)
        throw SocketError(400, "JSON doesn't contain \"action\" parameter.")

    val page = findPage(json.getString("action"))

    if (page != null)
        page.second.run {
            val time = measureTimeMillis {
                SocketSession(this@handleIncoming, json, user, page.first).receive()
            }

            socketLogger.info("${user.owner} -> ${json.getString("action")} (took ${time}ms)")
        }
    else {
        errorMessage("That page could not be found.")
    }
}

private fun findPage(action: String): Pair<SocketType, SocketAction>? {
    SocketActionHandler.socketActions.forEach { (type, page) ->
        if (action.equals(type.type, true)) return type to page
    }

    return null
}
