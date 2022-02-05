package net.unifey.handle.live

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.encodeToString
import net.unifey.Unifey
import net.unifey.handle.InvalidArguments
import net.unifey.handle.users.friends.updateFriendOffline
import net.unifey.handle.users.friends.updateFriendOnline
import org.json.JSONObject

/** Different classes can send out live information (such as notifications) */
object Live {
    data class Session(val channel: Channel<LiveObject>, val connectedAt: Long)
    data class LiveObject constructor(val type: String, val user: Long, val data: String) {
        constructor(type: String, user: Long, data: JSONObject) : this(type, user, data.toString())
    }

    /**
     * An online user's ID to their socket's channel.
     *
     * The channel allows for live notifications.
     */
    private val USERS_ONLINE = ConcurrentHashMap<Long, Session>()

    /** Send [obj] to a user on a socket. */
    suspend fun sendUpdate(obj: LiveObject) {
        socketLogger.trace("SEND ${obj.user} (ATTEMPT): ${obj.type} (${obj.data})")

        USERS_ONLINE[obj.user]?.channel?.send(obj)
    }

    data class LiveObjectBuilder(
        var type: String? = null,
        var user: Long? = null,
        var data: Any? = null
    )

    data class MultiLiveObjectBuilder(
        var type: String? = null,
        var users: List<Long>? = null,
        var data: Any? = null
    )

    /** Send multiple updates to users, using [builder]. */
    suspend fun sendUpdates(builder: MultiLiveObjectBuilder.() -> Unit) {
        val obj = MultiLiveObjectBuilder()

        builder.invoke(obj)

        if (obj.type == null || obj.users == null || obj.data == null) {
            throw InvalidArguments()
        }

        obj.users!!.forEach { user ->
            sendUpdate(LiveObject(obj.type!!, user, Unifey.JSON.encodeToString(obj.data)))
        }
    }

    /** Get [USERS_ONLINE] */
    fun getOnlineUsers() = USERS_ONLINE

    /**
     * [user] goes online. This sends updates to [user]'s friends.
     *
     * @param channel The channel from the websocket, to allow for live updates.
     */
    suspend fun userOnline(user: Long, channel: Channel<LiveObject>) {
        USERS_ONLINE[user] = Session(channel, System.currentTimeMillis())

        updateFriendOnline(user)
    }

    /** [user] goes offline. This sends updates to [user]'s friends. */
    suspend fun userOffline(user: Long) {
        USERS_ONLINE.remove(user)

        updateFriendOffline(user)
    }
}
