package net.unifey.auth

import net.unifey.DatabaseHandler
import net.unifey.auth.tokens.Token
import net.unifey.auth.tokens.TokenManager
import net.unifey.util.IdGenerator
import org.apache.commons.codec.digest.DigestUtils
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.streams.asSequence
import org.mindrot.jbcrypt.BCrypt

/**
 * Manages user authentication.
 */
object Authenticator {
    /**
     * If [username] has been previously used.
     */
    fun usernameTaken(username: String): Boolean =
            DatabaseHandler.getConnection()
                    .prepareStatement("SELECT * FROM users WHERE username = ?")
                    .apply { setString(1, username) }
                    .executeQuery()
                    .next()

    /**
     * If [email] has been previously used.
     */
    fun emailInUse(email: String): Boolean =
            DatabaseHandler.getConnection()
                    .prepareStatement("SELECT * FROM users WHERE email = ?")
                    .apply { setString(1, email) }
                    .executeQuery()
                    .next()

    /**
     * Generate a token if [username] and [password] are correct. If not, return null.
     */
    fun generateIfCorrect(username: String, password: String): Token? {
        val rs = DatabaseHandler.getConnection()
                .prepareStatement("SELECT * FROM users WHERE username = ?")
                .apply { setString(1, username) }
                .executeQuery()

        if (rs.next()) {
            val dbPassword = rs.getString("password")

            if (BCrypt.checkpw(password, dbPassword)) {
                val token = IdGenerator.generateToken()

                return TokenManager
                        .createToken(token, rs.getLong("id"), System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1))
            }
        }

        return null
    }
}