package net.unifey.auth.users

import net.unifey.DatabaseHandler
import net.unifey.auth.Authenticator
import net.unifey.feeds.Feed
import net.unifey.feeds.FeedManager
import net.unifey.util.IdGenerator
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
     * Get a user by their [name]. Prefers [userCache] over database.
     */
    fun getUser(name: String): User {
        val cacheUser = userCache.values
                .singleOrNull { it.username.equals(name, true) }

        if (cacheUser != null)
            return cacheUser

        val rs = DatabaseHandler.getConnection()
                .prepareStatement("SELECT created_at, username, email, uid, password, uid FROM users WHERE username = ?")
                .apply { setString(1, name) }
                .executeQuery()

        return if (rs.next()) {
            val user = User(
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("email"),
                    rs.getLong("uid"),
                    rs.getLong("created_at")
            )

            userCache[rs.getLong("uid")] = user

            user
        } else throw UserNotFound()
    }

    /**
     * Changes a users name in the database & cache
     */
    fun updateName(id: Long, name: String) {
        getUser(id) // to make sure user exists

        val stmt = DatabaseHandler.getConnection().prepareStatement("UPDATE users SET username = ? WHERE uid = ?")
        stmt.setString(1, name)
        stmt.setLong(2, id)
        stmt.executeQuery()
        userCache[id]?.username = name
    }

    /**
     * Changes a users email in the database & cache
     */
    fun updateEmail(id: Long, email: String) {
        getUser(id) // to make sure user exists

        val stmt = DatabaseHandler.getConnection().prepareStatement("UPDATE users SET email = ? WHERE uid = ?")
        stmt.setString(1, email)
        stmt.setLong(2, id)
        stmt.executeQuery()
        userCache[id]?.email = email
    }

    /**
     * Get a user by their [id]. Prefers [userCache] over database.
     */
    fun getUser(id: Long): User {
        if (userCache.containsKey(id))
            return userCache[id]!!

        val rs = DatabaseHandler.getConnection()
                .prepareStatement("SELECT created_at, email, username, password, uid FROM users WHERE uid = ?")
                .apply { setLong(1, id) }
                .executeQuery()

        return if (rs.next()) {
            val user = User(
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("email"),
                    rs.getLong("uid"),
                    rs.getLong("created_at")
            )

            userCache[id] = user

            user
        } else throw UserNotFound()
    }

    /**
     * To see if emails are proper.
     */
    val EMAIL_REGEX = Regex("(?:[a-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#\$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])")

    /**
     * If a users email and username are valid. If they are, return null, if they aren't, return the reason.
     */
    private fun inputValid(email: String, username: String): String? {
        return when {
            !EMAIL_REGEX.matches(email) ->
                "Invalid email!"

            email.length > 128 ->
                "Your email is too long!"

            username.length > 16 ->
                "Your username must be less than 16 characters!"

            3 > username.length ->
                "Your username must be more than 3 characters!"

            else ->
                null
        }
    }

    /**
     * Create an account with [email], [username] and [password].
     *
     * TODO return output from [inputValid]
     */
    fun createUser(email: String, username: String, password: String): Boolean {
        if (Authenticator.emailInUse(email) || Authenticator.usernameTaken(username) || inputValid(email, username) != null)
            return false

        val stmt = DatabaseHandler.getConnection()
                .prepareStatement("INSERT INTO users (created_at, email, username, password, uid) VALUES (?, ?, ?, ?, ?)")

        stmt.setLong(1, System.currentTimeMillis())
        stmt.setString(2, email)
        stmt.setString(3, username)

        val salt = IdGenerator.generateRandomString(8)
        val hashedPassword = DigestUtils.sha256Hex(password + salt)
        val finalPassword = "$salt:$hashedPassword"

        val id = IdGenerator.getId()

        stmt.setString(4, finalPassword)
        stmt.setLong(5, id)
        stmt.execute()

        FeedManager.createFeedForUser(id)

        return true
    }
}