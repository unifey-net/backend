package net.unifey.handle.notification

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import kotlinx.coroutines.channels.Channel
import net.unifey.handle.mongo.Mongo
import net.unifey.util.IdGenerator
import org.bson.Document
import java.util.concurrent.ConcurrentHashMap

object NotificationManager {
    val LIVE = Channel<Pair<Long, Notification>>()

    /**
     * Post a notification with [message] for [user].
     */
    suspend fun postNotification(user: Long, message: String) {
        val id = IdGenerator.getId() //TODO: make sure ID hasn't been used

        val notification = Notification(
            id,
            user,
            message,
            System.currentTimeMillis(),
            false
        )

        Mongo.getClient()
            .getDatabase("users")
            .getCollection("notifications")
            .insertOne(Document(mapOf(
                "user" to user,
                "id" to id,
                "message" to message,
                "date" to System.currentTimeMillis(),
                "read" to false
            )))

        LIVE.send(user to notification)
    }

    /**
     * Form a BSON [doc] into a [Notification] object.
     */
    private fun formNotificationObject(doc: Document): Notification {
        return Notification(
            doc.getLong("id"),
            doc.getLong("user"),
            doc.getString("message"),
            doc.getLong("date"),
            doc.getBoolean("read")
        )
    }

    /**
     * Get a [Notification] by it's [id].
     */
    fun getNotification(id: Long): Notification? {
        val find = Mongo.getClient()
            .getDatabase("users")
            .getCollection("notifications")
            .find(Filters.eq("id", id))
            .first()

        return if (find == null) null else formNotificationObject(find)
    }

    /**
     * Get all of [user]'s notifications. Any notifications over 50 are cut off, and can be requested after the initial are closed.
     */
    fun getNotifications(user: Long): List<Notification> {
        val find = Mongo.getClient()
            .getDatabase("users")
            .getCollection("notifications")
            .find(Filters.eq("user", user))

        return if (!find.any())
            emptyList()
        else {
            val notifs = find
                .map { doc -> formNotificationObject(doc) }
                .toList()

            if (notifs.size > 50) {
                notifs.subList(0, 50)
            } else notifs
        }
    }

    /**
     * Delete a [Notification].
     */
    fun deleteNotification(id: Long) {
        Mongo.getClient()
            .getDatabase("users")
            .getCollection("notifications")
            .deleteOne(Filters.eq("id", id))
    }

    /**
     * Delete all of [user]'s notifications.
     */
    fun deleteAllNotifications(user: Long) {
        Mongo.getClient()
            .getDatabase("users")
            .getCollection("notifications")
            .deleteMany(Filters.eq("user", user))
    }

    /**
     * Mark a notification as read.
     */
    fun readNotification(id: Long) {
        Mongo.getClient()
            .getDatabase("users")
            .getCollection("notifications")
            .updateOne(Filters.eq("id", id), Updates.set("read", true))
    }

    /**
     * Mark all notifications as read.
     */
    fun readAllNotifications(user: Long) {
        Mongo.getClient()
            .getDatabase("users")
            .getCollection("notifications")
            .updateMany(Filters.eq("user", user), Updates.set("read", true))
    }

    /**
     * Get all unread notifications for [user].
     */
    fun getAllUnreadNotifications(user: Long): Pair<Int, List<Notification>> {
        val find = Mongo.getClient()
            .getDatabase("users")
            .getCollection("notifications")
            .find(Filters.and(Filters.eq("user", user), Filters.eq("read", false)))

        return if (!find.any())
            0 to emptyList()
        else {
            val notifs = find
                .map { doc -> formNotificationObject(doc) }
                .toList()

            if (notifs.size > 50) {
                notifs.size to notifs.subList(0, 50)
            } else notifs.size to notifs
        }
    }
}