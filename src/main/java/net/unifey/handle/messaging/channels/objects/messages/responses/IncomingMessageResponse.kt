package net.unifey.handle.messaging.channels.objects.messages.responses

import net.unifey.handle.messaging.channels.objects.MessageChannel
import net.unifey.handle.messaging.channels.objects.messages.Message

/**
 * A sent message from the websocket
 */
data class IncomingMessageResponse(
    val channel: MessageChannel,
    val message: Message,
    val sentFrom: Pair<Long, String>
)