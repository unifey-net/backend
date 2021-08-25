package net.unifey.handle.live

import kotlinx.coroutines.channels.Channel
import net.unifey.handle.users.friends.updateFriendOffline
import net.unifey.handle.users.friends.updateFriendOnline
import org.json.JSONObject

/**
 * Different classes can send out live information (such as notifications)
 */
object Live {
    val CHANNEL = Channel<LiveObject>()

    data class LiveObject(val type: String, val user: Long, val data: JSONObject)

    private val USERS_ONLINE = arrayListOf<Long>()

    /**
     * Get [USERS_ONLINE]
     */
    fun getOnlineUsers() = USERS_ONLINE

    /**
     * [user] goes online
     */
    suspend fun userOnline(user: Long) {
        USERS_ONLINE.add(user)

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