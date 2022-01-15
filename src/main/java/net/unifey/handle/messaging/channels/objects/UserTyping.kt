package net.unifey.handle.messaging.channels.objects

/**
 * A typing user.
 *
 * @param user The user that's typing.
 * @param channel The channel the user's currently typing in.
 * @param startAt When the user started typing. This is used to stop the typing request after
 * inactivity.
 */
data class UserTyping(val user: Long, val channel: Long, val startAt: Long)
