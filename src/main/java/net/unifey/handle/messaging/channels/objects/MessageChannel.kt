package net.unifey.handle.messaging.channels.objects

import kotlinx.serialization.Serializable

/**
 * A message channel.
 *
 * @param id The ID of the channel.
 * @param channelType The type of channel. [ChannelType.DIRECT_MESSAGE] or [ChannelType.GROUP]
 */
@Serializable open class MessageChannel(open val id: Long, val channelType: ChannelType)
