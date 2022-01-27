package net.unifey.handle.messaging.channels.objects.responses

import net.unifey.handle.messaging.channels.objects.GroupMessageChannel
import net.unifey.handle.users.ShortUser

data class GroupChatOwnerResponse(
    val channel: GroupMessageChannel,
    val oldOwner: ShortUser,
    val newOwner: ShortUser
)
