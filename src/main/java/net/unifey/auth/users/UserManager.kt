package net.unifey.auth.users

import net.unifey.DatabaseHandler
import net.unifey.auth.Authenticator
import org.apache.commons.codec.digest.DigestUtils
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

    /**
     * Create an account with [email], [username] and [password].
     */
    fun createUser(email: String, username: String, password: String): Boolean {
        if (Authenticator.emailInUse(email) || Authenticator.usernameTaken(username))
            return false

        val stmt = DatabaseHandler.createConnection()
                .prepareStatement("INSERT INTO users (email, username, password, uid) VALUES (?, ?, ?, ?)")

        stmt.setString(1, email)
        stmt.setString(2, username)

        val salt = Authenticator.generateRandomString(8)
        val hashedPassword = DigestUtils.sha256Hex(password + salt)
        val finalPassword = "$salt:$hashedPassword"
        stmt.setString(3, finalPassword)
        stmt.setLong(4, Authenticator.generateId())
        stmt.execute()

        return true;
    }
}