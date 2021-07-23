package net.unifey.handle.notification

import dev.shog.lib.util.toJSON
import io.ktor.http.cio.websocket.*
import net.unifey.auth.tokens.Token
import net.unifey.auth.tokens.TokenManager
import net.unifey.handle.socket.WebSocket.authenticateMessage
import net.unifey.handle.socket.WebSocket.customTypeMessage
import net.unifey.handle.socket.WebSocket.errorMessage
import net.unifey.handle.socket.WebSocket.successMessage
import org.json.JSONObject

/**
 * Pages for the notification websocket.
 */
object NotificationSocketPages {
    /**
     * The authenticate websocket action.
     * A token must be sent as a parameter, and a [Token?] is returned. If unsuccessful, null will be returned.
     */
    suspend fun WebSocketSession.authenticate(json: JSONObject): Token? {
        if (!json.has("token")) {
            errorMessage("Missing \"token\" parameter.")
            return null
        } else {
            val tokenStr = json.getString("token")

            val token = TokenManager.getToken(tokenStr)

            if (token == null) {
                errorMessage("Failed to authenticate with provided token")
                return null
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
    suspend fun WebSocketSession.closeNotification(token: Token?, json: JSONObject): Boolean {
        when {
            token == null -> errorMessage("You must be authenticated.")
            !json.has("notification") -> errorMessage("Missing \"notification\" parameter.")

            else -> {
                val notification = json.getLong("notification")

                val obj = NotificationManager.getNotification(notification)

                if (obj == null) {
                    errorMessage("That notification couldn't be found.")
                    return false
                }

                if (token.owner == obj.user) {
                    NotificationManager.deleteNotification(notification)

                    successMessage("Successfully deleted notification.")
                } else {
                    errorMessage("You don't have permission with this notification!")
                    return false
                }
            }
        }

        return true
    }

    /**
     * Close all notifications.
     */
    suspend fun WebSocketSession.closeAllNotifications(token: Token?): Boolean {
        return when (token) {
            null -> {
                errorMessage("You must be authenticated.")
                false
            }
            else -> {
                NotificationManager.deleteAllNotifications(token.owner)

                successMessage("Successfully deleted all notifications")

                true
            }
        }
    }

    /**
     * Mark a notification as read through the websocket.
     */
    suspend fun WebSocketSession.readNotification(token: Token?, json: JSONObject): Boolean {
        when {
            token == null -> errorMessage("You must be authenticated.")
            !json.has("notification") -> errorMessage("Missing \"notification\" parameter.")

            else -> {
                val notification = json.getLong("notification")

                val obj = NotificationManager.getNotification(notification)

                if (obj == null) {
                    errorMessage("That notification couldn't be found.")
                    return false
                }

                if (token.owner == obj.user) {
                    NotificationManager.readNotification(notification)

                    successMessage("Successfully read notification.")
                    return true
                } else {
                    errorMessage("You don't have permission with this notification!")
                }
            }
        }
        return false
    }

    /**
     * Read all notifications.
     */
    suspend fun WebSocketSession.readAllNotifications(token: Token?): Boolean {
        when (token) {
            null -> errorMessage("You must be authenticated.")
            else -> {
                NotificationManager.readAllNotifications(token.owner)

                successMessage("Successfully read all notifications")
                return true
            }
        }

        return false
    }

    /**
     * Un-read a notification
     */
    suspend fun WebSocketSession.unReadNotification(token: Token?, json: JSONObject): Boolean {
        when {
            token == null -> errorMessage("You must be authenticated.")
            !json.has("notification") -> errorMessage("Missing \"notification\" parameter.")

            else -> {
                val notification = json.getLong("notification")

                val obj = NotificationManager.getNotification(notification)

                if (obj == null) {
                    errorMessage("That notification couldn't be found.")
                    return false
                }

                if (token.owner == obj.user) {
                    NotificationManager.unReadNotification(notification)

                    successMessage("Notification successfully unread.")
                    return true
                } else {
                    errorMessage("You don't have permission with this notification!")
                }
            }
        }

        return false
    }

    /**
     * Get all unread notifications, as well as a count of how many there are.
     */
    suspend fun WebSocketSession.getAllUnreadNotifications(token: Token?): Boolean {
        when (token) {
            null -> errorMessage("You must be authenticated.")
            else -> {
                val notifications = NotificationManager.getAllUnreadNotifications(token.owner)

                customTypeMessage("success_receive_unread",
                    JSONObject()
                        .put("notifications", notifications.second.map { notifs -> notifs.asJson() }.toJSON())
                        .put("count", notifications.first)
                )

                return true
            }
        }

        return false
    }

    /**
     * Get all notifications.
     */
    suspend fun WebSocketSession.getAllNotifications(token: Token?): Boolean {
        when (token) {
            null -> errorMessage("You must be authenticated.")
            else -> {
                val notifications = NotificationManager.getNotifications(token.owner)

                customTypeMessage("success_receive_all_notification", notifications.map { notif -> notif.asJson() }.toJSON())

                return true
            }
        }
        return false
    }
}