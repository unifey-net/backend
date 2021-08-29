package net.unifey.handle.live

import kotlinx.coroutines.channels.Channel
import net.unifey.handle.users.friends.updateFriendOffline
import net.unifey.handle.users.friends.updateFriendOnline
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Different classes can send out live information (such as notifications)
 */
object Live {
    data class LiveObject(val type: String, val user: Long, val data: JSONObject)

    /**
     * An online user's ID to their socket's channel.
     *
     * The channel allows for live notifications.
     */
    private val USERS_ONLINE = ConcurrentHashMap<Long, Channel<LiveObject>>()

    /**
     * Send [obj] to a user on a socket.
     */
    suspend fun sendUpdate(obj: LiveObject) {
        socketLogger.debug("SEND ${obj.user} (ATTEMPT): ${obj.type} (${obj.data})")

        USERS_ONLINE[obj.user]?.send(obj)
    }

    /**
     * Get [USERS_ONLINE]
     */
    fun getOnlineUsers() = USERS_ONLINE

    /**
     * [user] goes online. This sends updates to [user]'s friends.
     *
     * @param channel The channel from the websocket, to allow for live updates.
     */
    suspend fun userOnline(user: Long, channel: Channel<LiveObject>) {
        USERS_ONLINE[user] = channel

        updateFriendOnline(user)
    }

    /**
     * [user] goes offline. This sends updates to [user]'s friends.
     */
    suspend fun userOffline(user: Long) {
        USERS_ONLINE.remove(user)

        updateFriendOffline(user)
    }
}