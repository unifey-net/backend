package net.unifey.handle.messaging.channels.objects.messages.responses

import kotlinx.serialization.Serializable

/**
 * An incoming message history. Invoked by a request.
 *
 * @param channel The requested channel.
 * @param page The requested page.
 * @param maxPage The amount of pages in a message history.
 * @param messages The messages from the [page].
 */
@Serializable
data class MessageHistoryResponse(
    val channel: Long,
    val page: Int,
    val maxPage: Int,
    val messages: List<IncomingMessageResponse>
)
