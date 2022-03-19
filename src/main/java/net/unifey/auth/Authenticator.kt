package net.unifey.auth

import io.ktor.http.*
import io.ktor.response.*
import net.unifey.auth.tokens.Token
import net.unifey.auth.tokens.TokenManager
import net.unifey.handle.Error
import net.unifey.handle.InvalidArguments
import net.unifey.handle.mongo.MONGO
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.User
import net.unifey.response.Response
import net.unifey.util.IdGenerator
import org.mindrot.jbcrypt.BCrypt

/** Manages user authentication. */
object Authenticator {
    /** The default token expire time. (two days) */
    const val TOKEN_EXPIRE = 1000 * 60 * 60 * 24 * 2

    /** If [username] has been previously used. */
    suspend fun usernameTaken(username: String): Boolean =
        MONGO.getDatabase("users")
            .getCollection<User>("users")
            .find()
            .toList()
            .any { user -> user.username.equals(username, true) }

    /** If [email] has been previously used. */
    suspend fun emailInUse(email: String): Boolean =
        MONGO.getDatabase("users")
            .getCollection<User>("users")
            .find()
            .toList()
            .any { user -> user.email.equals(email, true) }

    /** Generate a token if [username] and [password] are correct. If not, return null. */
    @Throws(InvalidArguments::class)
    suspend fun generateIfCorrect(username: String, password: String, remember: Boolean): Token {
        val user = MONGO.getDatabase("users")
            .getCollection<User>("users")
            .find()
            .toList()
            .firstOrNull { user -> user.username.equals(username, true) }

        if (user != null) {
            if (BCrypt.checkpw(password, user.password)) {
                return generate(user.id, remember)
            }
        }

        throw Error({ respond(HttpStatusCode.Unauthorized, Response("Invalid credentials.")) })
    }

    /** Generate a token for [user] */
    suspend fun generate(user: Long, remember: Boolean = false): Token {
        val token = IdGenerator.generateToken()

        return TokenManager.createToken(
            token,
            user,
            if (remember) -1 else System.currentTimeMillis() + TOKEN_EXPIRE
        )
    }
}
