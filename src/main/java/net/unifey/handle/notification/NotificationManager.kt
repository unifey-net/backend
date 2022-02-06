package net.unifey.handle.notification

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.unifey.handle.live.Live
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.User
import net.unifey.util.IdGenerator
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.setValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object NotificationManager {
    val LOGGER: Logger = LoggerFactory.getLogger(this.javaClass)

    /** Post a notification with [message] for a [User]. */
    suspend fun User.postNotification(message: String) = postNotification(this.id, message)

    /** Post a notification with [message] for [user]. */
    suspend fun postNotification(user: Long, message: String) {
        val id = IdGenerator.getId() // TODO: make sure ID hasn't been used

        val notification = Notification(id, user, message, System.currentTimeMillis(), false)

        Mongo.K_MONGO
            .getDatabase("users")
            .getCollection<Notification>("notifications")
            .insertOne(notification)

        LOGGER.trace("CREATE NOTIFICATION: $user -> ${message.length}")

        Live.sendUpdate(Live.LiveObject("NOTIFICATION", user, Json.encodeToString(notification)))
    }

    /** Get a [Notification] by it's [id]. */
    suspend fun getNotification(id: Long): Notification? {
        return Mongo.K_MONGO
            .getDatabase("users")
            .getCollection<Notification>("notifications")
            .find(Notification::id eq id)
            .first()
    }

    /**
     * Get all of [user]'s notifications. Any notifications over 50 are cut off, and can be
     * requested after the initial are closed.
     */
    suspend fun getNotifications(user: Long): List<Notification> {
        val notifs =
            Mongo.K_MONGO
                .getDatabase("users")
                .getCollection<Notification>("notifications")
                .find(Notification::user eq user)
                .toList()

        return if (!notifs.any()) emptyList()
        else {
            if (notifs.size > 50) {
                notifs.subList(0, 50)
            } else notifs
        }
    }

    /** Delete a [Notification]. */
    suspend fun deleteNotification(id: Long) {
        Mongo.K_MONGO
            .getDatabase("users")
            .getCollection<Notification>("notifications")
            .deleteOne(Notification::id eq id)
    }

    /** Delete all of a [User]'s notifications */
    suspend fun User.deleteAllNotifications() = deleteAllNotifications(this.id)

    /** Delete all of [user]'s notifications. */
    suspend fun deleteAllNotifications(user: Long) {
        Mongo.K_MONGO
            .getDatabase("users")
            .getCollection<Notification>("notifications")
            .deleteOne(Notification::user eq user)
    }

    /** Mark a notification as read. */
    suspend fun readNotification(id: Long) {
        Mongo.K_MONGO
            .getDatabase("users")
            .getCollection<Notification>("notifications")
            .updateOne(Notification::id eq id, setValue(Notification::read, true))
    }

    /** Mark a notification as unread. */
    suspend fun unReadNotification(id: Long) {
        Mongo.K_MONGO
            .getDatabase("users")
            .getCollection<Notification>("notifications")
            .updateOne(Notification::id eq id, setValue(Notification::read, false))
    }

    /** Read all [User]'s notifications. */
    suspend fun User.readAllNotifications() = readAllNotifications(this.id)

    /** Mark all notifications as read. */
    suspend fun readAllNotifications(user: Long) {
        Mongo.K_MONGO
            .getDatabase("users")
            .getCollection<Notification>("notifications")
            .updateMany(Notification::user eq user, setValue(Notification::read, false))
    }

    /** Get a [User]'s unread notifications. */
    suspend fun User.getAllUnreadNotifications(): Pair<Int, List<Notification>> =
        getAllUnreadNotifications(this.id)

    /** Get all unread notifications for [user]. */
    suspend fun getAllUnreadNotifications(user: Long): Pair<Int, List<Notification>> {
        val notifs =
            Mongo.K_MONGO
                .getDatabase("users")
                .getCollection<Notification>("notifications")
                .find(and(Notification::user eq user, Notification::read eq false))
                .toList()

        return if (!notifs.any()) 0 to emptyList()
        else {
            if (notifs.size > 50) {
                notifs.size to notifs.subList(0, 50)
            } else notifs.size to notifs
        }
    }
}
