package net.unifey.handle.messaging.channels.objects.responses

import net.unifey.handle.messaging.channels.objects.MessageChannel

data class ChannelResponse(val channel: MessageChannel, val pageCount: Int, val messageCount: Int)