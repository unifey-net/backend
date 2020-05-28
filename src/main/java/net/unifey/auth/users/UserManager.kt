package net.unifey.auth.users

import net.unifey.DatabaseHandler
import java.util.concurrent.ConcurrentHashMap

object UserManager {
    /**
     * A cache of users.
     *
     * Key: UID
     */
    private val userCache = ConcurrentHashMap<Long, User>()

    /**
     * Get a user by their [id]. Prefers [userCache] over database.
     */
    fun getUser(id: Long): User? {
        if (userCache.containsKey(id))
            return userCache[id]!!

        val rs = DatabaseHandler.createConnection()
                .prepareStatement("SELECT created_at, email, username, password, uid FROM users WHERE uid = ?")
                .apply { setLong(1, id) }
                .executeQuery()

        return if (rs.next()) {
            val user = User(
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("email"),
                    rs.getLong("uid"),
                    rs.getString("created_at")
            )

            userCache[id] = user

            user
        } else null
    }
}