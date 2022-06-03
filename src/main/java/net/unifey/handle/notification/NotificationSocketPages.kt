package net.unifey.handle.notification

import kotlinx.serialization.Serializable
import net.unifey.handle.NotFound
import net.unifey.handle.live.SocketAction
import net.unifey.handle.live.SocketActionHandler
import net.unifey.handle.live.SocketActionHandler.action
import net.unifey.handle.live.WebSocket.errorMessage
import net.unifey.handle.live.WebSocket.successMessage
import net.unifey.handle.live.objs.ActionHolder
import net.unifey.handle.live.objs.FindActions
import net.unifey.handle.live.objs.SocketSession
import net.unifey.handle.live.objs.SocketType
import net.unifey.handle.live.respondSuccess

enum class NotificationSocketPagesTypes : SocketType {
    CLOSE_NOTIFICATION,
    CLOSE_ALL_NOTIFICATION,
    READ_NOTIFICATION,
    UN_READ_NOTIFICATION,
    READ_ALL_NOTIFICATION,
    GET_ALL_UNREAD_NOTIFICATION,
    GET_ALL_NOTIFICATIONS
}

/** Socket actions for notifications. */
@FindActions
object NotificationSocketPages : ActionHolder {
    override val pages: ArrayList<Pair<SocketType, SocketAction>> =
        SocketActionHandler.socketActions {
            /** Close a notification. */
            action(NotificationSocketPagesTypes.CLOSE_NOTIFICATION) {
                checkArguments("notification" to Long::class)

                val notification = data.getLong("notification")

                val obj = NotificationManager.getNotification(notification)

                if (obj == null) {
                    errorMessage("That notification couldn't be found.")
                } else {
                    if (token.owner == obj.user) {
                        NotificationManager.deleteNotification(notification)

                        successMessage("Successfully deleted notification.")
                    } else {
                        errorMessage("You don't have permission with this notification!")
                    }
                }
            }

            /** Delete all notifications */
            action(NotificationSocketPagesTypes.CLOSE_ALL_NOTIFICATION) {
                NotificationManager.deleteAllNotifications(token.owner)

                successMessage("Successfully deleted all notifications")
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
            action(NotificationSocketPagesTypes.READ_NOTIFICATION) {
                val obj = getNotification() ?: throw NotFound("notification")

                if (token.owner == obj.user) {
                    NotificationManager.readNotification(obj.id)

                    successMessage("Successfully read notification.")
                } else {
                    errorMessage("You don't have permission with this notification!")
                }
            }

            /** Un-read a notification */
            action(NotificationSocketPagesTypes.UN_READ_NOTIFICATION) {
                val obj = getNotification() ?: throw NotFound("notification")

                if (token.owner == obj.user) {
                    NotificationManager.unReadNotification(obj.id)

                    successMessage("Notification successfully unread.")
                } else {
                    errorMessage("You don't have permission with this notification!")
                }
            }

            /** Read all notifications. */
            action(NotificationSocketPagesTypes.READ_ALL_NOTIFICATION) {
                NotificationManager.readAllNotifications(token.owner)

                successMessage("Successfully read all notifications")
            }

            /** Get all unread notifications and the count. */
            action(NotificationSocketPagesTypes.GET_ALL_UNREAD_NOTIFICATION) {
                @Serializable
                data class NotificationResponse(
                    val notifications: List<Notification>,
                    val count: Int
                )

                val (count, notifications) =
                    NotificationManager.getAllUnreadNotifications(token.owner)

                respondSuccess(NotificationResponse(notifications, count))
            }

            /** Get all notifications. */
            action(NotificationSocketPagesTypes.GET_ALL_NOTIFICATIONS) {
                val notifications = NotificationManager.getNotifications(token.owner)

                respondSuccess(notifications)
            }
        }
}
