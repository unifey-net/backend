package net.unifey.handle.users.profile

import com.fasterxml.jackson.annotation.JsonIgnore
import com.mongodb.client.model.Filters.eq
import net.unifey.handle.mongo.Mongo
import org.bson.Document

/**
 * A user's profile.
 */
class Profile(
        @JsonIgnore
        val id: Long,
        description: String,
        discord: String,
        location: String
) {
    /**
     * A user's Discord.
     *
     * TODO verify this using Discord
     */
    var discord = discord
        set(value) {
            Mongo.getClient()
                    .getDatabase("users")
                    .getCollection("profiles")
                    .updateOne(eq("id", id), Document(mapOf(
                            "discord" to value
                    )))

            field = value
        }

    /**
     * A user's description.
     */
    var description = description
        set(value) {
            Mongo.getClient()
                    .getDatabase("users")
                    .getCollection("profiles")
                    .updateOne(eq("id", id), Document(mapOf(
                            "description" to value
                    )))

            field = value
        }

    /**
     * A user's location. This is adjustable by user, and is not accurate.
     */
    var location = location
        set(value) {
            Mongo.getClient()
                    .getDatabase("users")
                    .getCollection("profiles")
                    .updateOne(eq("id", id), Document(mapOf(
                            "location" to value
                    )))

            field = value
        }
}