package net.unifey.handle.users.responses

import net.unifey.handle.users.User

data class FriendResponse(
    val friend: Long,
    val friendedAt: Long,
    val friendDetails: User
)