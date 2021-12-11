package net.unifey.handle.messaging.channels.objects.messages.responses

import net.unifey.handle.messaging.channels.objects.messages.Message

data class MessageHistoryResponse(val channel: Long, val page: Int, val maxPage: Int, val messages: List<IncomingMessageResponse>)