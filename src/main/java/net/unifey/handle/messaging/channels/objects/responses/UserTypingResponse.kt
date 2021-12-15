package net.unifey.handle.messaging.channels.objects.responses

import net.unifey.handle.messaging.channels.objects.MessageChannel
import net.unifey.handle.users.ShortUser

data class UserTypingResponse(
    val user: ShortUser,
    val channel: MessageChannel
)