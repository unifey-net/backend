package net.unifey.handle.messaging.channels.objects.messages

import kotlinx.serialization.Serializable

/**
 * A message. Resides in a [MessageChannel].
 *
 * @param user The user that sent the message.
 * @param id The unique ID of the message.
 * @param channel Where the message was sent.
 * @param message The message contents itself.
 * @param time The time the message was sent.
 * @param reactions The reactions on the message. List of IDs of Emojis
 */
@Serializable
data class Message(
    val user: Long,
    val id: Long,
    val channel: Long,
    val message: String,
    val time: Long,
    val reactions: List<Long>
) {
    companion object {
        /** The ID of the system user. */
        @Transient const val SYSTEM_ID = 0L

        /** The system user's name. */
        @Transient const val SYSTEM_NAME = "Harold"
    }
}
