package net.unifey.handle.users.profile.cosmetics

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.unifey.handle.NotFound
import net.unifey.handle.mongo.MONGO
import net.unifey.handle.users.profile.Profile
import net.unifey.util.URL
import org.litote.kmongo.and
import org.litote.kmongo.eq

object Cosmetics {
    @Serializable
    data class Cosmetic(val type: Int, val id: String, val desc: String)

    /** Get all available cosmetics. */
    suspend fun getAll(): List<Cosmetic> {
        return MONGO.getDatabase("global").getCollection<Cosmetic>("cosmetics").find().toList()
    }

    /** Get [user]'s [Cosmetic]'s */
    suspend fun getCosmetics(user: Long): List<Cosmetic> {
        val cosmetics =
            MONGO
                .getDatabase("users")
                .getCollection<Profile>("profiles")
                .findOne(Profile::id eq user)

        if (cosmetics != null) {
            return cosmetics.cosmetics
        } else throw NotFound("profile")
    }
    /** Upload a cosmetic. */
    suspend fun uploadCosmetic(type: Int, id: String, desc: String) {
        MONGO
            .getDatabase("global")
            .getCollection<Cosmetic>("cosmetics")
            .insertOne(Cosmetic(type, id, desc))
    }

    /** Delete a cosmetic */
    suspend fun deleteCosmetic(type: Int, id: String) {
        MONGO
            .getDatabase("global")
            .getCollection<Cosmetic>("cosmetics")
            .deleteOne(and(Cosmetic::type eq type, Cosmetic::id eq id))
    }

    fun List<Cosmetic>.hasCosmetic(id: String, type: Int): Boolean = any { cos ->
        cos.type == type && id.equals(id, true)
    }
}
