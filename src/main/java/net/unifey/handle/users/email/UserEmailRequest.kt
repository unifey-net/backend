package net.unifey.handle.users.email

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates
import net.unifey.handle.mongo.Mongo

/**
 * An email request.
 *
 * An email request can be for a password reset or for verifying that the email exists.
 */
class UserEmailRequest(
        val id: Long?,
        val email: String,
        val verify: String,
        val type: Int,
        attempts: Int
) {
    /**
     * The amount of times the email has been resent.
     */
    var attempts = attempts
        set(value) {
            Mongo.getClient()
                    .getDatabase("email")
                    .getCollection("verify")
                    .updateOne(Filters.and(eq("id", id), eq("verify", verify)), Updates.set("attempts", value))

            field = value
        }
}