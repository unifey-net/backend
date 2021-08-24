package net.unifey.handle.users.responses

import net.unifey.handle.users.User
import net.unifey.handle.users.friends.FriendRequest

data class ReceivedFriendRequestResponse(
    val friendRequest: FriendRequest,
    val receivedFrom: User
)