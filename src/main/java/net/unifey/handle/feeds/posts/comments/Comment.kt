package net.unifey.handle.feeds.posts.comments

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import net.unifey.handle.feeds.posts.Post
import net.unifey.handle.feeds.posts.PostAttributes
import net.unifey.handle.mongo.Mongo
import org.bson.Document

/**
 * A comment. This could be on an actual [Post] or on another [Comment]. This depends on [level].
 */
class Comment(
        val parent: Long,
        val level: Int,
        val id: Long,
        val createdAt: Long,
        val authorId: Long,
        val feed: String,
        content: String,
        upvotes: Long,
        downvotes: Long
) {
    /**
     * The comment's attributes.
     */
    private val attributes = PostAttributes(id, "comments")

    /**
     * If the post is pinned.
     */
    var pinned: Boolean
        get() = attributes.getAttribute("pinned")
        set(value) = attributes.setAttribute("pinned", value)

    /**
     * If the post is hidden.
     */
    var hidden: Boolean
        get() = attributes.getAttribute("hidden")
        set(value) = attributes.setAttribute("hidden", value)

    /**
     * Change a post's content
     */
    var content = content
        set(value) {
            Mongo.getClient()
                    .getDatabase("feeds")
                    .getCollection("comments")
                    .updateOne(Filters.eq("id", id), Updates.set("content", content))

            field = value
        }

    /**
     * A post's upvotes
     */
    var upvotes = upvotes
        set(value) {
            Mongo.getClient()
                    .getDatabase("feeds")
                    .getCollection("comments")
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
                    .getCollection("comments")
                    .updateOne(Filters.eq("id", id), Updates.set("vote.downvotes", value))

            field = value
        }
}