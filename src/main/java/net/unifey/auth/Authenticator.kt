package net.unifey.auth

import net.unifey.DatabaseHandler
import net.unifey.auth.tokens.Token
import net.unifey.auth.tokens.TokenManager
import net.unifey.util.IdGenerator
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
                val token = IdGenerator.generateToken()

                return TokenManager
                        .createToken(token, rs.getLong("id"), System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1))
            }
        }

        return null
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