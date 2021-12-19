package net.unifey.handle.messaging.channels.objects

data class DirectMessageChannel(override val id: Long, val users: ArrayList<Long>) :
    MessageChannel(id, ChannelType.DIRECT_MESSAGE)
