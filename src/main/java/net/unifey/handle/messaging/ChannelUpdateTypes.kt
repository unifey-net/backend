package net.unifey.handle.messaging

import net.unifey.handle.live.objs.SocketType

enum class ChannelUpdateTypes : SocketType {
    REMOVE_GROUP_CHAT_USER,
    GROUP_CHAT_CHANGE_DESCRIPTION,
    GROUP_CHAT_CHANGE_OWNER,
    GROUP_CHAT_CHANGE_NAME,
    MESSAGE_HISTORY
}
