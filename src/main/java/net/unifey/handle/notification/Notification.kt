package net.unifey.handle.notification

import org.json.JSONObject

/**
 * A user's notification
 */
class Notification(val id: Long, val user: Long, val message: String, val date: Long, read: Boolean) {
    /**
     * Close notifications
     */
    fun close() {
        NotificationManager.deleteNotification(this.id)
    }

    /**
     * If the notifications been read.
     */
    var read = read
        set(value) {
            NotificationManager.readNotification(this.id)
            field = value
        }

    /**
     * Get the notification as json. Used in websocket.
     */
    fun asJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("message", message)
            .put("user", user)
            .put("read", read)
            .put("date", date)
    }
}