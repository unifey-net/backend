package net.unifey.handle.messaging.channels.objects.responses

import net.unifey.handle.messaging.channels.objects.GroupMessageChannel
import net.unifey.handle.users.ShortUser

/**
 * The response when a user is kicked from a group chat [channel].
 *
 * @param channel The channel where the user was kicked.
 * @param user The user that was kicked.
 */
data class GroupChatKickResponse(val channel: GroupMessageChannel, val user: ShortUser)
