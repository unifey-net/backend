package net.unifey.handle.messaging.channels.objects.responses

import net.unifey.handle.messaging.channels.objects.GroupMessageChannel
import net.unifey.handle.users.ShortUser

data class GroupChatKickResponse(val channel: GroupMessageChannel, val user: ShortUser)