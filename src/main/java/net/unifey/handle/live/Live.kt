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
    private val USERS_ONLINE = ConcurrentHashMap<Long, Channel<LiveObject>>()

    /**
     * Send update.
     */
    suspend fun sendUpdate(obj: LiveObject) {
        socketLogger.debug("SEND ${obj.user} (ATTEMPT): ${obj.type} (${obj.data})")
        socketLogger.debug(USERS_ONLINE.toString())

        USERS_ONLINE[obj.user]?.send(obj)
    }

    data class LiveObject(val type: String, val user: Long, val data: JSONObject)

    /**
     * Get [USERS_ONLINE]
     */
    fun getOnlineUsers() = USERS_ONLINE

    /**
     * [user] goes online
     */
    suspend fun userOnline(user: Long, channel: Channel<LiveObject>) {
        USERS_ONLINE[user] = channel

        updateFriendOnline(user)
    }

    /**
     * [user] goes offline
     */
    suspend fun userOffline(user: Long) {
        USERS_ONLINE.remove(user)

        updateFriendOffline(user)
    }
}