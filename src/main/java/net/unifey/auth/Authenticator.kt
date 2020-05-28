package net.unifey.auth

import net.unifey.DatabaseHandler
import net.unifey.auth.tokens.Token
import net.unifey.auth.tokens.TokenManager
import org.apache.commons.codec.digest.DigestUtils
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.streams.asSequence

/**
 * Manages user authentication.
 */
object Authenticator {
    /**
     * If [username] has been previously used.
     */
    fun usernameTaken(username: String): Boolean {
        val stmt = DatabaseHandler.getConnection()
                .prepareStatement("SELECT * FROM users WHERE username = ?")

        stmt.setString(1, username)
        stmt.execute()

        return stmt.resultSet.fetchSize > 0
    }

    /**
     * If [email] has been previously used.
     */
    fun emailInUse(email: String): Boolean {
        val stmt = DatabaseHandler.getConnection()
                .prepareStatement("SELECT * FROM users WHERE email = ?")

        stmt.setString(1, email)
        stmt.execute()

        return stmt.resultSet.fetchSize > 0
    }

    /**
     * Generate a token if [username] and [password] are correct. If not, return null.
     */
    fun generateIfCorrect(username: String, password: String): Token? {
        val stmt = DatabaseHandler.getConnection()
                .prepareStatement("SELECT * FROM users WHERE username = ?")

        stmt.setString(1, username)

        val rs = stmt.executeQuery()

        if (rs.next()) {
            val databasePassword = rs.getString("password")
            val chunks = databasePassword.split(":");

            val salt = chunks[0]
            val hash = chunks[1]

            if (DigestUtils.sha256Hex(password + salt) == hash) {
                val token = generateToken()

                return TokenManager
                        .createToken(token, rs.getLong("uid"), System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1))
            }
        }

        return null
    }

    /**
     * Generate a token.
     */
    private fun generateToken(): String {
        val token = DigestUtils.sha256Hex(generateRandomString(32))
        if (tokenUsed(token))
            return generateToken()
        return token
    }

    private fun tokenUsed(token: String): Boolean {
        return TokenManager.getToken(token) != null
    }

    /**
     * Generate a random string [len].
     */
    fun generateRandomString(len: Long): String {
        val source = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        return java.util.Random().ints(len, 0, source.length)
                .asSequence()
                .map(source::get)
                .joinToString("")
    }

    /**
     * Generate an ID.
     */
    fun generateId(): Long {
        val id = generateRandomLong(18)
        if (uidTaken(id))
            return generateId()
        return id
    }

    /**
     * Generate a random Long with the given [len].
     */
    private fun generateRandomLong(len: Int): Long {
        var idStr = ""
        for (i in 0 until len)
            idStr += Random.nextInt(10).toString()
        return idStr.toLong()
    }

    /**
     * Check if the [uid] is in use.
     */
    private fun uidTaken(uid: Long): Boolean {
        val stmt = DatabaseHandler.getConnection()
                .prepareStatement("SELECT * FROM users WHERE uid = ?")

        stmt.setLong(1, uid)
        stmt.execute()

        return stmt.resultSet.fetchSize > 0
    }
}