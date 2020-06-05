package net.unifey.handle.users

import net.unifey.DatabaseHandler
import net.unifey.auth.Authenticator
import net.unifey.handle.NotFound
import net.unifey.handle.feeds.FeedManager
import net.unifey.util.IdGenerator
import org.apache.commons.codec.digest.DigestUtils
import java.util.concurrent.ConcurrentHashMap

/**
 * Manage [User]s.
 */
object UserManager {
    private val cache = ConcurrentHashMap<Long, User>()

    /**
     * Get a user's ID by their [name].
     */
    fun getId(name: String): Long {
        val rs = DatabaseHandler.getConnection()
                .prepareStatement("SELECT id FROM users WHERE username = ?")
                .apply { setString(1, name) }
                .executeQuery()

        if (rs.next())
            return rs.getLong("id")
        else throw NotFound("user")
    }

    /**
     * Get a user by their [id]. Prefers [userCache] over database.
     */
    fun getUser(id: Long): User {
        if (cache.containsKey(id))
            return cache[id]!!

        val rs = DatabaseHandler.getConnection()
                .prepareStatement("SELECT * FROM users WHERE id = ?")
                .apply { setLong(1, id) }
                .executeQuery()

        return if (rs.next()) {
            val user = User(
                    rs.getLong("id"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("email"),
                    rs.getInt("role"),
                    rs.getLong("created_at")
            )

            cache[id] = user

            user
        } else throw NotFound("user")
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
                .prepareStatement("INSERT INTO users (created_at, email, username, password, id) VALUES (?, ?, ?, ?, ?)")

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