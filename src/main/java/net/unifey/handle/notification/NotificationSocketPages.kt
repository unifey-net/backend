package net.unifey.handle.notification

import dev.ajkneisl.lib.util.toJSON
import io.ktor.http.cio.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.unifey.handle.live.SocketActionHandler
import net.unifey.handle.live.SocketActionHandler.action
import net.unifey.handle.live.SocketSession
import net.unifey.handle.live.WebSocket.customTypeMessage
import net.unifey.handle.live.WebSocket.errorMessage
import net.unifey.handle.live.WebSocket.successMessage
import org.json.JSONObject

/** Socket actions for notifications. */
fun notificationSocketActions() =
    SocketActionHandler.socketActions {
        /** Close a notification. */
        action("CLOSE_NOTIFICATION") {
            if (!data.has("notification") || data["notification"] !is Long) {
                errorMessage("Missing \"notification\" parameter.")
                return@action false
            }

            val notification = data.getLong("notification")

            val obj = NotificationManager.getNotification(notification)

            if (obj == null) {
                errorMessage("That notification couldn't be found.")
                return@action false
            }

            if (token.owner == obj.user) {
                NotificationManager.deleteNotification(notification)

                successMessage("Successfully deleted notification.")
                return@action true
            } else {
                errorMessage("You don't have permission with this notification!")
                return@action false
            }
        }

        /** Delete all notifications */
        action("CLOSE_ALL_NOTIFICATION") {
            NotificationManager.deleteAllNotifications(token.owner)

            successMessage("Successfully deleted all notifications")

            true
        }

        /** Get a notification from [data]. */
        suspend fun SocketSession.getNotification(): Notification? {
            if (!data.has("notification") && data["notification"] !is Long) {
                errorMessage("Missing \"notification\" parameter.")
                return null
            }

            val notification = data.getLong("notification")

            val obj = NotificationManager.getNotification(notification)

            if (obj == null) {
                errorMessage("That notification couldn't be found.")
                return null
            }

            return obj
        }

        /** Read a notification. */
        action("READ_NOTIFICATION") {
            val obj = getNotification() ?: return@action false

            if (token.owner == obj.user) {
                NotificationManager.readNotification(obj.id)

                successMessage("Successfully read notification.")
                return@action true
            } else {
                errorMessage("You don't have permission with this notification!")
                return@action false
            }
        }

        /** Un-read a notification */
        action("UN_READ_NOTIFICATION") {
            val obj = getNotification() ?: return@action false

            if (token.owner == obj.user) {
                NotificationManager.unReadNotification(obj.id)

                successMessage("Notification successfully unread.")
                return@action true
            } else {
                errorMessage("You don't have permission with this notification!")
                return@action true
            }
        }

        /** Read all notifications. */
        action("READ_ALL_NOTIFICATION") {
            NotificationManager.readAllNotifications(token.owner)
            successMessage("Successfully read all notifications")
            true
        }

        /** Get all unread notifications and the count. */
        action("GET_ALL_UNREAD_NOTIFICATION") {
            val (count, notifications) = NotificationManager.getAllUnreadNotifications(token.owner)

            customTypeMessage(
                "SUCCESS_RECEIVE_UNREAD",
                JSONObject()
                    .put(
                        "notifications",
                        notifications.map { notifs -> Json.encodeToString(notifs) }.toJSON())
                    .put("count", count))

            true
        }

        /** Get all notifications. */
        action("GET_ALL_NOTIFICATION") {
            val notifications = NotificationManager.getNotifications(token.owner)

            customTypeMessage(
                "SUCCESS_RECEIVE_ALL_NOTIFICATION",
                notifications.map { notif -> Json.encodeToString(notif) }.toJSON())

            true
        }
    }
