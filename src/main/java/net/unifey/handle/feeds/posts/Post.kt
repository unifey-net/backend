package net.unifey.handle.feeds.posts

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import net.unifey.handle.mongo.Mongo
import org.bson.Document

/**
 * A post.
 *
 * @param id The ID of the post.
 * @param createdAt When the post was created.
 * @param authorId The ID of the user who created the post.
 * @param feed The ID of the feed the post was created in.
 */
class Post(
        val id: Long,
        val createdAt: Long,
        val authorId: Long,
        val feed: String,
        title: String,
        content: String,
        hidden: Boolean,
        upvotes: Long,
        downvotes: Long
) {
    /**
     * Change a post's title.
     */
    var title = title
        set(value) {
            Mongo.getClient()
                    .getDatabase("feeds")
                    .getCollection("posts")
                    .updateOne(Filters.eq("id", id), Document(mapOf(
                            "title" to value
                    )))

            field = value
        }

    /**
     * Change a post's content
     */
    var content = content
        set(value) {
            Mongo.getClient()
                    .getDatabase("feeds")
                    .getCollection("posts")
                    .updateOne(Filters.eq("id", id), Document(mapOf(
                            "content" to value
                    )))

            field = value
        }

    /**
     * If the post is hidden.
     */
    var hidden = hidden
        set(value) {
            Mongo.getClient()
                    .getDatabase("feeds")
                    .getCollection("posts")
                    .updateOne(Filters.eq("id", id), Document(mapOf(
                            "hidden" to value
                    )))

            field = value
        }

    /**
     * A post's upvotes
     */
    var upvotes = upvotes
        set(value) {
            Mongo.getClient()
                    .getDatabase("feeds")
                    .getCollection("posts")
                    .updateOne(Filters.eq("id", id), Updates.set("vote.upvotes", value))

            field = value
        }

    /**
     * A post's downvotes
     */
    var downvotes = downvotes
        set(value) {
            Mongo.getClient()
                    .getDatabase("feeds")
                    .getCollection("posts")
                    .updateOne(Filters.eq("id", id), Updates.set("vote.downvotes", value))

            field = value
        }
}