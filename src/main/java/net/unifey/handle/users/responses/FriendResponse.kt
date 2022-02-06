package net.unifey.handle.users.responses

import kotlinx.serialization.Serializable
import net.unifey.handle.users.User

/** A response when getting a friend. */
@Serializable
data class FriendResponse(val friend: Long, val friendedAt: Long, val friendDetails: User)
