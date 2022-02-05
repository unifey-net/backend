package net.unifey.handle.messaging.channels.objects.responses

import kotlinx.serialization.Serializable
import net.unifey.handle.messaging.channels.objects.MessageChannel
import net.unifey.handle.users.ShortUser

/**
 * When a user stops or starts typing.
 *
 * @param user The user that's starting/stopping.
 * @param channel The channel where the user started/stopped typing.
 */
@Serializable
data class UserTypingResponse(val user: ShortUser, val channel: MessageChannel)
