package net.unifey.handle.users

import com.mongodb.client.model.Filters.eq
import net.unifey.handle.InvalidVariableInput
import net.unifey.handle.NotFound
import net.unifey.handle.feeds.FeedManager
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.email.UserEmailManager
import net.unifey.util.IdGenerator
import org.bson.Document
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
     *
     * TODO caching
     */
    fun getId(name: String): Long {
        val collection = Mongo.getClient()
                .getDatabase("users")
                .getCollection("users")

        val user = getUser(
                collection
                        .find(eq("username", name))
                        .firstOrNull()
        )

        return user.id
    }

    /**
     * Get a user by their [id]. Prefers [cache] over database.
     */
    @Throws(NotFound::class)
    fun getUser(id: Long): User {
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
    fun createUser(email: String, username: String, password: String): User {
        InputRequirements.allMeets(username, password, email)

        val id = IdGenerator.getId()

        val userDocument = Document(mapOf(
                "id" to id,
                "username" to username,
                "password" to BCrypt.hashpw(password, BCrypt.gensalt()),
                "email" to email,
                "created_at" to System.currentTimeMillis(),
                "verified" to false,
                "role" to 0
        ))

        Mongo.getClient()
                .getDatabase("users")
                .getCollection("users")
                .insertOne(userDocument)

        FeedManager.createFeedForUser(id)

        UserEmailManager.sendVerify(id, email)

        return getUser(id)
    }
}