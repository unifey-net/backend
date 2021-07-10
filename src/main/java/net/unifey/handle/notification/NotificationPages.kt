package net.unifey.handle.notification

import dev.shog.lib.util.toJSON
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import net.unifey.auth.tokens.Token
import net.unifey.auth.tokens.TokenManager
import net.unifey.handle.socket.WebSocket.authenticateMessage
import net.unifey.handle.socket.WebSocket.customTypeMessage
import net.unifey.handle.socket.WebSocket.errorMessage
import net.unifey.handle.socket.WebSocket.successMessage
import org.json.JSONObject

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
                            when (json.getString("action").toUpperCase()) {
                                NotificationWebsocketAction.AUTHENTICATE -> token = authenticate(json)

                                NotificationWebsocketAction.CLOSE_NOTIFICATION -> closeNotification(token, json)
                                NotificationWebsocketAction.CLOSE_ALL_NOTIFICATION -> closeAllNotifications(token)

                                NotificationWebsocketAction.READ_NOTIFICATION -> readNotification(token, json)
                                NotificationWebsocketAction.READ_ALL_NOTIFICATION -> readAllNotifications(token)

                                NotificationWebsocketAction.GET_ALL_UNREAD_NOTIFICATION -> getAllUnreadNotifications(token)
                                NotificationWebsocketAction.GET_ALL_NOTIFICATION -> getAllNotifications(token)
                            }
                        }
                    }
                }
            }

            if (token != null)
                connectedTokens.remove(token.owner)
        }
    }
}

/**
 * The authenticate websocket action.
 * A token must be sent as a parameter, and a [Token?] is returned. If unsuccessful, null will be returned.
 */
private suspend fun WebSocketSession.authenticate(json: JSONObject): Token? {
    if (!json.has("token")) {
        errorMessage("Missing \"token\" parameter.")
        return null
    } else {
        val tokenStr = json.getString("token")

        val token = TokenManager.getToken(tokenStr)

        if (token == null) {
            errorMessage("Failed to authenticate with provided token")
        } else {
            authenticateMessage(token.owner, token.expires)
            connectedTokens.add(token.owner)
        }

        return token
    }
}

/**
 * The close notification websocket action.
 */
private suspend fun WebSocketSession.closeNotification(token: Token?, json: JSONObject) {
    when {
        token == null -> errorMessage("You must be authenticated.")
        !json.has("notification") -> errorMessage("Missing \"notification\" parameter.")

        else -> {
            val notification = json.getLong("notification")

            val obj = NotificationManager.getNotification(notification)

            if (obj == null) {
                errorMessage("That notification couldn't be found.")
                return
            }

            if (token.owner == obj.user) {
                NotificationManager.deleteNotification(notification)

                successMessage("Successfully deleted notification.")
            } else {
                errorMessage("You don't have permission with this notification!")
            }
        }
    }
}

/**
 * Close all notifications.
 */
private suspend fun WebSocketSession.closeAllNotifications(token: Token?) {
    when (token) {
        null -> errorMessage("You must be authenticated.")
        else -> {
            NotificationManager.deleteAllNotifications(token.owner)

            successMessage("Successfully deleted all notifications")
        }
    }
}

/**
 * Mark a notification as read through the websocket.
 */
private suspend fun WebSocketSession.readNotification(token: Token?, json: JSONObject) {
    when {
        token == null -> errorMessage("You must be authenticated.")
        !json.has("notification") -> errorMessage("Missing \"notification\" parameter.")

        else -> {
            val notification = json.getLong("notification")

            val obj = NotificationManager.getNotification(notification)

            if (obj == null) {
                errorMessage("That notification couldn't be found.")
                return
            }

            if (token.owner == obj.user) {
                NotificationManager.readNotification(notification)

                successMessage("Successfully read notification.")
            } else {
                errorMessage("You don't have permission with this notification!")
            }
        }
    }
}

/**
 * Read all notifications.
 */
private suspend fun WebSocketSession.readAllNotifications(token: Token?) {
    when (token) {
        null -> errorMessage("You must be authenticated.")
        else -> {
            NotificationManager.readAllNotifications(token.owner)

            successMessage("Successfully read all notifications")
        }
    }
}

/**
 * Get all unread notifications, as well as a count of how many there are.
 */
private suspend fun WebSocketSession.getAllUnreadNotifications(token: Token?) {
    when (token) {
        null -> errorMessage("You must be authenticated.")
        else -> {
            val notifications = NotificationManager.getAllUnreadNotifications(token.owner)

            customTypeMessage("success_receive_unread",
                JSONObject()
                    .put("notifications", notifications.second.map { notifs -> notifs.asJson() }.toJSON())
                    .put("count", notifications.first)
            )
        }
    }
}

/**
 * Get all notifications.
 */
private suspend fun WebSocketSession.getAllNotifications(token: Token?) {
    when (token) {
        null -> errorMessage("You must be authenticated.")
        else -> {
            val notifications = NotificationManager.getNotifications(token.owner)

            customTypeMessage("success_receive_all_notification", notifications.map { notif -> notif.asJson() }.toJSON())
        }
    }
}