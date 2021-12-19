package net.unifey.handle.feeds.posts

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import net.unifey.handle.MalformedContent
import net.unifey.handle.NotFound
import net.unifey.handle.mongo.Mongo
import org.bson.Document

/** Post attributes for [id]. */
class PostAttributes(val id: Long, private val collection: String = "posts") {
    private val store: MutableMap<String, Any> by lazy {
        val doc =
            Mongo.getClient()
                .getDatabase("feeds")
                .getCollection(collection)
                .find(Filters.eq("id", id))
                .firstOrNull()
                ?: throw NotFound("post")

        val store = doc["attributes"] as? Document ?: throw MalformedContent()

        store.toMap().toMutableMap()
    }

    /** Set [attr] to [value] */
    fun setAttribute(attr: String, value: Any) {
        Mongo.getClient()
            .getDatabase("feeds")
            .getCollection(collection)
            .updateOne(Filters.eq("id"), Updates.set("attributes.$attr", value))

        store[attr] = value
    }

    /** Get [attr] as [T] */
    fun <T> getAttribute(attr: String, default: T): T = store[attr] as? T ?: default
}
