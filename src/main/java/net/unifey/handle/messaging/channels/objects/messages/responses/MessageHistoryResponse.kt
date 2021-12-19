package net.unifey.handle.messaging.channels.objects.messages.responses

data class MessageHistoryResponse(
    val channel: Long,
    val page: Int,
    val maxPage: Int,
    val messages: List<IncomingMessageResponse>
)
