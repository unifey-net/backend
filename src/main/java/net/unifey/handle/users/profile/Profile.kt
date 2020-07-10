package net.unifey.handle.users.profile

import com.fasterxml.jackson.annotation.JsonIgnore
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.profile.cosmetics.Cosmetics
import org.bson.Document

/**
 * A user's profile.
 */
class Profile(
        @JsonIgnore
        val id: Long,
        description: String,
        discord: String,
        location: String,
        cosmetics: List<Cosmetics.Cosmetic>
) {
    companion object {
        const val MAX_DESC_LEN = 256
        const val MAX_LOC_LEN = 32
        const val MAX_DISC_LEN = 64
    }

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
                    .updateOne(eq("id", id), Updates.set("discord", value))

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
                    .updateOne(eq("id", id), Updates.set("description", value))

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
                    .updateOne(eq("id", id), Updates.set("location", value))

            field = value
        }

    /**
     * A user's cosmetics
     */
    var cosmetics = cosmetics
        set(value) {
            Mongo.getClient()
                    .getDatabase("users")
                    .getCollection("profiles")
                    .updateOne(eq("id", id), Updates.set("cosmetics", value.map { it.toDocument() }))

            field = value
        }
}