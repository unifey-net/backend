package net.unifey.handle.users

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import net.unifey.auth.tokens.TokenManager
import net.unifey.auth.tokens.TokenManager.deleteToken
import net.unifey.handle.InvalidVariableInput
import net.unifey.handle.NotFound
import net.unifey.handle.feeds.FeedManager
import net.unifey.handle.live.Live
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.notification.NotificationManager
import net.unifey.handle.users.email.UserEmailManager
import net.unifey.util.IdGenerator
import org.bson.Document
import org.json.JSONObject
import org.mindrot.jbcrypt.BCrypt
import java.util.concurrent.ConcurrentHashMap

/**
 * Manage [User]s.
 */
object UserManager {
    private val cache = ConcurrentHashMap<Long, User>()

    /**
     * Get a [User] object from the [bson] document.
     */
    @Throws(NotFound::class)
    fun getUser(bson: Document?): User {
        if (bson == null)
            throw NotFound("user")

        val user = User(
                bson.getLong("id"),
                bson.getString("username"),
                bson.getString("password"),
                bson.getString("email"),
                bson.getInteger("role"),
                bson.getBoolean("verified"),
                bson.getLong("created_at")
        )

        cache[user.id] = user

        return user
    }

    /**
     * Get a user's ID by their [name].
     */
    suspend fun getId(name: String): Long {
        val cacheUser = cache
                .filter { user -> user.value.username.equals(name, true) }
                .keys
                .firstOrNull()

        if (cacheUser != null)
            return cacheUser

        val collection = Mongo.getClient()
                .getDatabase("users")
                .getCollection("users")

        val user = getUser(
                collection
                        .find()
                        .firstOrNull { doc -> doc.getString("username").equals(name, true) }
        )

        return user.id
    }

    /**
     * Get a user by their [id]. Prefers [cache] over database.
     */
    @Throws(NotFound::class)
    suspend fun getUser(id: Long): User {
        if (cache.containsKey(id))
            return cache[id]!!

        val collection = Mongo.getClient()
                .getDatabase("users")
                .getCollection("users")

        return getUser(
                collection
                        .find(eq("id", id))
                        .firstOrNull()
        )
    }

    /**
     * Create an account with [email], [username] and [password].
     */
    @Throws(InvalidVariableInput::class)
    suspend fun createUser(email: String, username: String, password: String, verified: Boolean = false): User {
        UserInputRequirements.allMeets(username, password, email)

        val id = IdGenerator.getId()

        val userDocument = Document(mapOf(
                "id" to id,
                "username" to username,
                "password" to BCrypt.hashpw(password, BCrypt.gensalt()),
                "email" to email,
                "created_at" to System.currentTimeMillis(),
                "verified" to verified,
                "role" to 0
        ))

        Mongo.getClient()
                .getDatabase("users")
                .getCollection("users")
                .insertOne(userDocument)

        FeedManager.createFeedForUser(id)

        if (!verified) {
            UserEmailManager.sendVerify(id, email)
            NotificationManager.postNotification(id, "A verification link has been sent to your email. If you can't see it, visit your settings to resend.")
        }

        return getUser(id)
    }

    /**
     * Signs out all users connected to the account (aka delete all tokens)
     */
    suspend fun signOutAll(user: User) {
        TokenManager.getTokensForUser(user)
            .forEach(::deleteToken)

        Live.sendUpdate(Live.LiveObject("SIGN_OUT", user.id, JSONObject()))
    }
}