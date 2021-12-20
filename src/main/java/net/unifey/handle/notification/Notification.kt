package net.unifey.handle.notification

/** A user's notification */
data class Notification(
    val id: Long,
    val user: Long,
    val message: String,
    val date: Long,
    val read: Boolean
)