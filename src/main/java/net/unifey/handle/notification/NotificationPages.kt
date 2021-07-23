package net.unifey.handle.notification

import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.unifey.auth.tokens.Token
import net.unifey.handle.notification.NotificationSocketPages.authenticate
import net.unifey.handle.notification.NotificationSocketPages.closeAllNotifications
import net.unifey.handle.notification.NotificationSocketPages.closeNotification
import net.unifey.handle.notification.NotificationSocketPages.getAllNotifications
import net.unifey.handle.notification.NotificationSocketPages.getAllUnreadNotifications
import net.unifey.handle.notification.NotificationSocketPages.readAllNotifications
import net.unifey.handle.notification.NotificationSocketPages.readNotification
import net.unifey.handle.notification.NotificationSocketPages.unReadNotification
import net.unifey.handle.socket.WebSocket.customTypeMessage
import net.unifey.handle.socket.WebSocket.errorMessage
import org.json.JSONObject
import org.slf4j.LoggerFactory

/**
 * The logger for the socket.
 */
private val websocketLogger = LoggerFactory.getLogger("notif-socket")

/**
 * A list of all connections to the websocket. This is basically all users on the website.
 */
val connectedTokens = arrayListOf<Long>()

/**
 * This is the websocket implementation as well as the REST implementation.
 */
fun Routing.notificationPages() {
    route("/notifications") {
        webSocket("/live") {
            var token: Token? = null

            // notification listener TODO: Probably better way to implement
            launch {
                while (!NotificationManager.LIVE.isClosedForReceive) {
                    val (user, notification) = NotificationManager.LIVE.receive()

                    if (connectedTokens.contains(user) && token?.owner == user) {
                        websocketLogger.info("SEND $user: LIVE NOTIF (${notification.id})")
                        customTypeMessage("notification", notification.asJson())
                    }
                }
            }

            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val data = String(frame.data)

                        val json = try {
                            JSONObject(data)
                        } catch (ex: Exception) {
                            null
                        }

                        if (json == null || !json.has("action")) {
                            errorMessage("Invalid action.")
                        } else {
                            val action = json.getString("action").toUpperCase()

                            val success = when (action) {
                                NotificationWebsocketAction.AUTHENTICATE -> {
                                    val receivedToken = authenticate(json)
                                    token = receivedToken
                                    receivedToken == null
                                }

                                NotificationWebsocketAction.CLOSE_NOTIFICATION -> closeNotification(token, json)
                                NotificationWebsocketAction.CLOSE_ALL_NOTIFICATION -> closeAllNotifications(token)

                                NotificationWebsocketAction.UN_READ_NOTIFICATION -> unReadNotification(token, json)
                                NotificationWebsocketAction.READ_NOTIFICATION -> readNotification(token, json)
                                NotificationWebsocketAction.READ_ALL_NOTIFICATION -> readAllNotifications(token)

                                NotificationWebsocketAction.GET_ALL_UNREAD_NOTIFICATION -> getAllUnreadNotifications(token)
                                NotificationWebsocketAction.GET_ALL_NOTIFICATION -> getAllNotifications(token)

                                else -> false
                            }

                            websocketLogger.info("RECEIVE (${token?.owner}): ${if (success) "OK" else "FAIL"} $action ")
                        }
                    }
                }
            }

            if (token != null)
                connectedTokens.remove(token.owner)
        }
    }
}