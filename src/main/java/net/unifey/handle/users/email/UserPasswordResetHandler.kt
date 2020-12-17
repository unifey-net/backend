package net.unifey.handle.users.email

import net.unifey.handle.NotFound
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.UserManager

object UserPasswordResetHandler {
    /**
     * Find a user's input based on an [input]. [input] can be either an email or username.
     */
    suspend fun findUsingInput(input: String): Long {
        return (if (input.contains("@"))
            findUsingEmail(input)
        else
            findUsingUsername(input))
                ?: throw NotFound("user")
    }

    /**
     * Find a user by their username ([input])
     */
    private suspend fun findUsingUsername(input: String): Long? {
        val result = Mongo.useAsync {
            getDatabase("users")
                    .getCollection("users")
                    .find()
                    .filter { doc -> doc.getString("username").equals(input, true) }
        }.await()

        val document = result.firstOrNull()
                ?: return null

        return UserManager.getUser(document).id
    }

    /**
     * Find user by their email ([input])
     */
    private suspend fun findUsingEmail(input: String): Long? {
        val result = Mongo.useAsync {
            getDatabase("users")
                    .getCollection("users")
                    .find()
                    .filter { doc -> doc.getString("email").equals(input, true) }
        }.await()

        val document = result.firstOrNull()
                ?: return null

        return UserManager.getUser(document).id
    }
}