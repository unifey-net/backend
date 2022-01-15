package net.unifey.handle.messaging.channels.objects.responses

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import net.unifey.handle.messaging.channels.objects.MessageChannel

/**
 * A response of a channel.
 *
 * @param channel The channel object.
 * @param pageCount The amount of pages of messages.
 * @param messageCount The amount of messages in the channel.
 */
@Serializable
data class ChannelResponse(
    @Polymorphic val channel: MessageChannel,
    val pageCount: Int,
    val messageCount: Int
)
