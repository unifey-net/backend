package net.unifey.handle.users.friends

import kotlinx.serialization.Serializable

@Serializable
data class FriendRequest(val sentAt: Long, val sentTo: Long, val sentFrom: Long)
