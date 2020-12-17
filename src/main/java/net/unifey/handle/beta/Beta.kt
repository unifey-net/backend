package net.unifey.handle.beta

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import net.unifey.handle.AlreadyExists
import net.unifey.handle.NotFound
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.UserInputRequirements
import net.unifey.handle.users.email.UserEmailManager
import org.bson.Document
import java.util.concurrent.TimeUnit

/**
 * Sign up for the beta.
 */
object Beta {
    /**
     * All signups.
     */
    private val signups: MutableMap<String, BetaSignup> by lazy {
        Mongo.getClient()
                .getDatabase("global")
                .getCollection("beta")
                .find()
                .map { doc ->
                    doc.getString("username") to BetaSignup(
                            doc.getString("username"),
                            doc.getString("email"),
                            doc.getLong("date"),
                            doc.getBoolean("verified")
                    )
                }
                .toMap()
                .toMutableMap()
    }

    fun isVerified(email: String): Boolean =
            signups
                    .filter { entry -> entry.value.email.equals(email, true) }
                    .any { entry -> entry.value.verified }

    @Throws(AlreadyExists::class)
    fun usernameExists(name: String) {
        val signup = signups
                .filter { entry -> entry.value.username.equals(name, true) }
                .keys
                .firstOrNull()

        if (signup != null) {
            val obj = signups[signup]!!

            if (obj.verified || System.currentTimeMillis() - obj.date < TimeUnit.DAYS.toMillis(7))
                throw AlreadyExists("account", "name")
        }
    }

    @Throws(AlreadyExists::class)
    fun emailExists(email: String) {
        val signup = signups
                .filter { entry -> entry.value.email.equals(email, true) }
                .keys
                .firstOrNull()

        if (signup != null) {
            val obj = signups[signup]!!

            if (!obj.verified && System.currentTimeMillis() - obj.date > TimeUnit.DAYS.toMillis(7))
                throw AlreadyExists("account", "email")
        }
    }

    /**
     * Sign up a user with [username] and [email].
     */
    suspend fun signUp(username: String, email: String) {
        // check if exists
        usernameExists(username)
        emailExists(email)

        // check if meets regex
        UserInputRequirements.meets(listOf(
                username to UserInputRequirements.USERNAME,
                email to UserInputRequirements.EMAIL
        ))

        val signup = BetaSignup(
                username,
                email,
                System.currentTimeMillis(),
                false
        )

        signups[signup.username] = signup

        Mongo.getClient()
                .getDatabase("global")
                .getCollection("beta")
                .insertOne(Document(mapOf(
                        "username" to signup.username,
                        "email" to signup.email,
                        "date" to signup.date,
                        "verified" to signup.verified
                )))

        UserEmailManager.sendBetaVerify(signup.email)
    }

    /**
     * Verify the beta account [email].
     */
    @Throws(NotFound::class)
    fun verify(email: String) {
        val signup = signups.values.singleOrNull { signup -> signup.email.equals(email, true) }
                ?: throw NotFound("beta signup")

        signup.verified = true

        Mongo.getClient()
                .getDatabase("global")
                .getCollection("beta")
                .updateOne(
                        Filters.eq("email", email),
                        Updates.set("verified", true)
                )
    }
}