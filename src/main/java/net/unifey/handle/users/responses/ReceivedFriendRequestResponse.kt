package net.unifey.handle.users.responses

import kotlinx.serialization.Serializable
import net.unifey.handle.users.User
import net.unifey.handle.users.friends.FriendRequest

@Serializable
data class ReceivedFriendRequestResponse(val friendRequest: FriendRequest, val sentFrom: User)
