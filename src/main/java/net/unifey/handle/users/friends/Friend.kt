package net.unifey.handle.users.friends

/**
 * An individual friend.
 *
 * @param id The ID of the friend.
 * @param friendedAt When the user was friended.
 */
data class Friend(val id: Long, val friendedAt: Long)
