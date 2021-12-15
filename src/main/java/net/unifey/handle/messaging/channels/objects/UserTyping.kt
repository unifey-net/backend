package net.unifey.handle.messaging.channels.objects

data class UserTyping(val user: Long, val channel: Long, val startAt: Long)