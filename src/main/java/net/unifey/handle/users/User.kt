package net.unifey.handle.users

import com.fasterxml.jackson.annotation.JsonIgnore
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.member.Member
import net.unifey.handle.users.profile.Profile
import net.unifey.handle.users.profile.cosmetics.Cosmetics
import org.bson.Document

class User(
        val id: Long,
        username: String,
        password: String,
        email: String,
        role: Int,
        verified: Boolean,
        val createdAt: Long
) {
    /**
     * A user's profile. This contains profile details such as Discord.
     */
    val profile by lazy {
        val doc = Mongo.getClient()
                .getDatabase("users")
                .getCollection("profiles")
                .find(Filters.eq("id", id))
                .singleOrNull()

        if (doc != null) {
            Profile(
                    id,
                    doc.getString("description"),
                    doc.getString("discord"),
                    doc.getString("location"),
                    Cosmetics.getCosmetics(id)
            )
        } else {
            Mongo.getClient()
                    .getDatabase("users")
                    .getCollection("profiles")
                    .insertOne(Document(mapOf(
                            "id" to id,
                            "discord" to "",
                            "location" to "",
                            "description" to "A Unifey user.",
                            "cosmetics" to listOf<Document>()
                    )))

            Profile(id, "A Unifey user.", "", "", listOf())
        }
    }


    /**
     * A user's memberships.
     */
    val member by lazy {
        val doc = Mongo.getClient()
                .getDatabase("users")
                .getCollection("members")
                .find(Filters.eq("id", id))
                .singleOrNull()

        if (doc != null) {
            Member(
                    id,
                    doc.getList("member", Long::class.java)
            )
        } else {
            Mongo.getClient()
                    .getDatabase("users")
                    .getCollection("members")
                    .insertOne(Document(mapOf(
                            "id" to id,
                            "member" to listOf<Long>()
                    )))

            Member(id, mutableListOf())
        }
    }

    /**
     * If the user's email is verified
     */
    var verified = verified
        set(value) {
            Mongo.getClient()
                    .getDatabase("users")
                    .getCollection("users")
                    .updateOne(Filters.eq("id", id), Updates.set("verified", value))

            field = value
        }

    /**
     * A user's global role.
     */
    var role = role
        set(value) {
            Mongo.getClient()
                    .getDatabase("users")
                    .getCollection("users")
                    .updateOne(Filters.eq("id", id), Updates.set("role", value))

            field = value
        }

    /**
     * A user's unique username. This is also their URL.
     */
    var username = username
        set(value) {
            Mongo.getClient()
                    .getDatabase("users")
                    .getCollection("users")
                    .updateOne(Filters.eq("id", id), Updates.set("username", value))

            field = value
        }

    /**
     * A user's email
     */
    @JsonIgnore
    var email = email
        set(value) {
            // delete all current email requests (password resets etc)
            Mongo.getClient()
                    .getDatabase("email")
                    .getCollection("verify")
                    .deleteMany(Filters.eq("id", id))

            Mongo.getClient()
                    .getDatabase("users")
                    .getCollection("users")
                    .updateOne(Filters.eq("id", id), Updates.set("email", value))

            field = value
        }

    /**
     * A user's password
     */
    @JsonIgnore
    var password = password
        set(value) {
            Mongo.getClient()
                    .getDatabase("users")
                    .getCollection("users")
                    .updateOne(Filters.eq("id", id), Updates.set("password", value))

            field = value
        }
}