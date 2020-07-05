package net.unifey.auth

import net.unifey.auth.tokens.Token
import net.unifey.auth.tokens.TokenManager
import net.unifey.handle.InvalidArguments
import net.unifey.handle.NotFound
import net.unifey.handle.mongo.Mongo
import net.unifey.util.IdGenerator
import java.util.concurrent.TimeUnit
import org.mindrot.jbcrypt.BCrypt

/**
 * Manages user authentication.
 */
object Authenticator {
    /**
     * If [username] has been previously used.
     */
    fun usernameTaken(username: String): Boolean =
            Mongo.getClient()
                    .getDatabase("users")
                    .getCollection("users")
                    .find()
                    .any { doc -> doc.getString("username").equals(username, true) }

    /**
     * If [email] has been previously used.
     */
    fun emailInUse(email: String): Boolean =
            Mongo.getClient()
                    .getDatabase("users")
                    .getCollection("users")
                    .find()
                    .any { doc -> doc.getString("email").equals(email, true) }

    /**
     * Generate a token if [username] and [password] are correct. If not, return null.
     */
    @Throws(InvalidArguments::class)
    fun generateIfCorrect(username: String, password: String): Token? {
        val user = Mongo.getClient()
                .getDatabase("users")
                .getCollection("users")
                .find()
                .firstOrNull { doc -> doc.getString("username").equals(username, true) }

        if (user != null) {
            val dbPassword = user.getString("password")

            if (BCrypt.checkpw(password, dbPassword)) {
                val token = IdGenerator.generateToken()

                return TokenManager.createToken(
                        token,
                        user.getLong("id"),
                        System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)
                )
            }
        }

        throw InvalidArguments("username", "password")
    }
}