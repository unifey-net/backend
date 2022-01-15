package net.unifey.handle.messaging

import net.unifey.handle.live.objs.SocketType

/** All actions for Channels and Messages */
enum class ChannelActions : SocketType {
    MODIFY_GROUP_CHAT,
    GET_CHANNELS,
    GET_CHANNEL,
    CREATE_GROUP_CHAT,
    OPEN_DIRECT_MESSAGE,
    STOP_TYPING,
    START_TYPING,
    DELETE_MESSAGE,
    SEND_MESSAGE,
    GET_MESSAGES,
}
