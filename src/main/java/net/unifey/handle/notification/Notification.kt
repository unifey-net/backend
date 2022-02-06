package net.unifey.handle.notification

import kotlinx.serialization.Serializable

/** A user's notification */
@Serializable
data class Notification(
    val id: Long,
    val user: Long,
    val message: String,
    val date: Long,
    val read: Boolean
)
