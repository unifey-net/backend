package net.unifey.handle.messaging.channels.objects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A direct message channel.
 *
 * @param id Randomly generated ID for the channel.
 * @param users Two users.
 */
@SerialName("directMessageChannel")
@Serializable
data class DirectMessageChannel(@Transient override val id: Long = 0, val users: ArrayList<Long>) :
    MessageChannel(id, ChannelType.DIRECT_MESSAGE)
