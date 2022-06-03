package net.unifey.handle.users.profile

import net.unifey.handle.NotFound
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.profile.cosmetics.Cosmetics
import org.litote.kmongo.*

/** Manage a [User]'s [Profile]. */
object ProfileManager {
    /** Get a user's profile by their [id] */
    @Throws(NotFound::class)
    suspend fun getProfile(id: Long): Profile {
        return Mongo.K_MONGO
            .getDatabase("users")
            .getCollection<Profile>("profiles")
            .findOne(Profile::id eq id)
            ?: throw NotFound("profile")
    }

    /** Set [id]'s [description] */
    suspend fun setDescription(id: Long, description: String) {
        Mongo.K_MONGO
            .getDatabase("users")
            .getCollection<Profile>("profiles")
            .updateOne(Profile::id eq id, setValue(Profile::description, description))
    }

    /** Set [id]'s [discord] */
    suspend fun setDiscord(id: Long, discord: String) {
        Mongo.K_MONGO
            .getDatabase("users")
            .getCollection<Profile>("profiles")
            .updateOne(Profile::id eq id, setValue(Profile::discord, discord))
    }

    /** Set [id]'s [setLocation] */
    suspend fun setLocation(id: Long, location: String) {
        Mongo.K_MONGO
            .getDatabase("users")
            .getCollection<Profile>("profiles")
            .updateOne(Profile::id eq id, setValue(Profile::location, location))
    }

    /** Add a [cosmetic] to [id] */
    suspend fun addCosmetic(id: Long, cosmetic: String) {
        Mongo.K_MONGO
            .getDatabase("users")
            .getCollection<Profile>("profiles")
            .updateOne(Profile::id eq id, push(Profile::cosmetics, cosmetic))
    }

    /** Add a [cosmetic] to [id] */
    suspend fun removeCosmetic(id: Long, cosmetic: String) {
        Mongo.K_MONGO
            .getDatabase("users")
            .getCollection<Profile>("profiles")
            .updateOne(Profile::id eq id, pull(Profile::cosmetics, cosmetic))
    }
}
