package net.unifey.handle.users.email

import net.unifey.handle.NotFound
import net.unifey.handle.mongo.MONGO
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.User
import net.unifey.handle.users.UserManager

object UserPasswordResetHandler {
    /** Find a user's input based on an [input]. [input] can be either an email or username. */
    suspend fun findUsingInput(input: String): Long {
        return (if (input.contains("@")) findUsingEmail(input) else findUsingUsername(input))
            ?: throw NotFound("user")
    }

    /** Find a user by their username ([input]) */
    private suspend fun findUsingUsername(input: String): Long? {
        return try {
            UserManager.getId(input)
        } catch (notFound: NotFound) {
            return null
        }
    }

    /** Find user by their email ([input]) */
    private suspend fun findUsingEmail(input: String): Long? {
        val result = MONGO.getDatabase("users")
            .getCollection<User>("users")
            .find()
            .toList()
            .singleOrNull { user -> user.email.equals(input, true) }

        return result?.id
    }
}
