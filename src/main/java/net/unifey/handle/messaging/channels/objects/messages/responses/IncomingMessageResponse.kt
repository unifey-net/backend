package net.unifey.handle.messaging.channels.objects.messages.responses

import kotlinx.serialization.Serializable
import net.unifey.handle.messaging.channels.objects.MessageChannel
import net.unifey.handle.messaging.channels.objects.messages.Message

/** A sent message from the websocket */
@Serializable
data class IncomingMessageResponse(
    val channel: MessageChannel,
    val message: Message,
    val sentFrom: Pair<Long, String>
)
