package net.unifey.handle.users.profile.cosmetics

import com.mongodb.client.model.Filters
import net.unifey.handle.NotFound
import net.unifey.handle.mongo.Mongo
import net.unifey.util.URL
import org.bson.Document

object Cosmetics {
    sealed class Cosmetic {
        abstract val type: Int
        abstract val id: String
        abstract val desc: String

        fun toDocument(): Document {
            return Document(mapOf("type" to type, "id" to id, "desc" to desc))
        }

        class Badge(override val id: String, override val desc: String) : Cosmetic() {
            override val type: Int = 0

            val image = "${URL}/user/cosmetic/viewer?type=${type}&id=${id}"
        }
    }

    /** Get all available cosmetics. */
    fun getAll(): List<Cosmetic> {
        return Mongo.getClient()
            .getDatabase("global")
            .getCollection("cosmetics")
            .find()
            .map { doc -> parseCosmetic(doc) }
            .toList()
    }

    /** Get [user]'s [Cosmetic]'s */
    fun getCosmetics(user: Long): List<Cosmetic> {
        val cosmetics =
            Mongo.getClient()
                .getDatabase("users")
                .getCollection("profiles")
                .find(Filters.eq("id", user))
                .singleOrNull()

        if (cosmetics != null) {
            val objs = cosmetics["cosmetics"] as MutableList<Document>

            return objs.map { doc -> parseCosmetic(doc) }
        } else throw NotFound("profile")
    }

    /** Parse a [Cosmetic] from [document]. */
    private fun parseCosmetic(document: Document): Cosmetic =
        when (document.getInteger("type")) {
            0 -> Cosmetic.Badge(document.getString("id"), document.getString("desc"))
            else -> throw NotFound("cosmetic")
        }

    /** Upload a cosmetic. */
    fun uploadCosmetic(type: Int, id: String, desc: String) {
        Mongo.getClient()
            .getDatabase("global")
            .getCollection("cosmetics")
            .insertOne(Document(mapOf("type" to type, "id" to id, "desc" to desc)))
    }

    /** Delete a cosmetic */
    fun deleteCosmetic(type: Int, id: String) {
        Mongo.getClient()
            .getDatabase("global")
            .getCollection("cosmetics")
            .deleteOne(Filters.and(Filters.eq("type", type), Filters.eq("id", id)))
    }
}
