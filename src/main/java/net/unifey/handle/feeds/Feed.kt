package net.unifey.handle.feeds

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.client.model.Filters
import net.unifey.handle.feeds.FeedManager.POSTS_PAGE_SIZE
import net.unifey.handle.mongo.Mongo
import org.bson.Document
import kotlin.math.ceil

class Feed(
        val id: String,
        val banned: MutableList<Long>,
        val moderators: MutableList<Long>,
        val postCount: Long,
        val pageCount: Long = ceil(postCount.toDouble() / POSTS_PAGE_SIZE.toDouble()).toLong()
) {
    /**
     * Update this feeds data to the database
     */
    fun update() {
        Mongo.getClient()
                .getDatabase("feeds")
                .getCollection("feeds")
                .updateOne(Filters.eq("id", id), Document(mapOf(
                        "banned" to banned,
                        "moderators" to moderators
                )))
    }
}