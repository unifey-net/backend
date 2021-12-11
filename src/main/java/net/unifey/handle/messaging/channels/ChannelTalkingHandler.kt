package net.unifey.handle.messaging.channels

import java.util.concurrent.ConcurrentHashMap

object ChannelTalkingHandler {
    val TYPING = ConcurrentHashMap<Long, ArrayList<Long>>()
}