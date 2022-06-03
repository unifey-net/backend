package net.unifey.handle.users.profile.cosmetics

import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import net.unifey.handle.AlreadyExists
import net.unifey.handle.NotFound
import net.unifey.handle.mongo.MONGO
import net.unifey.handle.users.profile.Profile
import org.litote.kmongo.and
import org.litote.kmongo.eq

object Cosmetics {
    val logger = KotlinLogging.logger {}

    val cosmetics: MutableList<Cosmetic> by lazy {
        logger.debug("Loading cosmetics...")

        val cosmetics = runBlocking {
            MONGO
                .getDatabase("global")
                .getCollection<Cosmetic>("cosmetics")
                .find()
                .toList()
                .toMutableList()
        }

        logger.debug("Found ${cosmetics.size} cosmetics.")

        cosmetics
    }

    @Serializable @JsonTypeInfo(use = JsonTypeInfo.Id.NAME) sealed class Cosmetic(val id: String)

    class Badge(id: String, val description: String) : Cosmetic(id)

    /** Get all available cosmetics. */
    fun getAll(): List<Cosmetic> {
        return cosmetics
    }

    @Throws(NotFound::class)
    fun getById(id: String): Cosmetic {
        return cosmetics.singleOrNull { cos -> cos.id == id } ?: throw NotFound("cosmetic")
    }

    /** Get [user]'s [Cosmetic]'s */
    suspend fun getCosmetics(user: Long): List<Cosmetic> {
        val cosmetics =
            MONGO
                .getDatabase("users")
                .getCollection<Profile>("profiles")
                .findOne(Profile::id eq user)

        if (cosmetics != null) {
            return cosmetics.cosmetics.map { id -> getById(id) }
        } else throw NotFound("profile")
    }

    /** Upload a cosmetic. */
    @Throws(AlreadyExists::class)
    suspend fun uploadCosmetic(cosmetic: Cosmetic) {
        if (cosmetics.any { cos -> cos.id == cosmetic.id }) throw AlreadyExists("cosmetic", "id")

        MONGO.getDatabase("global").getCollection<Cosmetic>("cosmetics").insertOne(cosmetic)
    }

    /** Delete a cosmetic */
    suspend fun deleteCosmetic(id: String) {
        MONGO
            .getDatabase("global")
            .getCollection<Cosmetic>("cosmetics")
            .deleteOne(Cosmetic::id eq id)
    }
}
