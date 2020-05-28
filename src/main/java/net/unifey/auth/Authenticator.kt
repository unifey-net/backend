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
     * Create an account with [email], [username] and [password].
     *
     * TODO make sure username is proper size & move to [UserManager]
     */
    fun createAccount(email: String, username: String, password: String): Boolean {
        if (emailInUse(email) || usernameTaken(username))
            return false

        val stmt = DatabaseHandler.createConnection()
                .prepareStatement("INSERT INTO users (email, username, password, uid) VALUES (?, ?, ?, ?)")

        stmt.setString(1, email)
        stmt.setString(2, username)

        val salt = generateRandomString(8)
        val hashedPassword = DigestUtils.sha256Hex(password + salt)
        val finalPassword = "$salt:$hashedPassword"
        stmt.setString(3, finalPassword)
        stmt.setLong(4, generateId())
        stmt.execute()

        return true;
    }

    /**
     * If [username] has been previously used.
     */
    private fun usernameTaken(username: String): Boolean {
        val stmt = DatabaseHandler.createConnection()
                .prepareStatement("SELECT * FROM users WHERE username = ?")

        stmt.setString(1, username)
        stmt.execute()

        return stmt.resultSet.fetchSize > 0
    }

    /**
     * If [email] has been previously used.
     */
    private fun emailInUse(email: String): Boolean {
        val stmt = DatabaseHandler.createConnection()
                .prepareStatement("SELECT * FROM users WHERE email = ?")

        stmt.setString(1, email)
        stmt.execute()

        return stmt.resultSet.fetchSize > 0
    }

    /**
     * Generate a token if [username] and [password] are correct. If not, return null.
     */
    fun generateIfCorrect(username: String, password: String): Token? {
        val stmt = DatabaseHandler.createConnection()
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
     *
     * TODO make sure it's not taken
     */
    private fun generateToken(): String =
            DigestUtils.sha256Hex(generateRandomString(32))

    /**
     * Generate a random string [len].
     */
    private fun generateRandomString(len: Long): String {
        val source = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        return java.util.Random().ints(len, 0, source.length)
                .asSequence()
                .map(source::get)
                .joinToString("")
    }

    /**
     * Generate an ID.
     *
     * TODO Make sure it's not taken
     */
    private fun generateId(): Long {
        var idStr = ""

        for (i in 0 until 18)
            idStr += Random.nextInt(10).toString()

        return idStr.toLong()
    }
}