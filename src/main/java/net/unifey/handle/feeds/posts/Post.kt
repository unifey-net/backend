package net.unifey.handle.feeds.posts

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import net.unifey.handle.feeds.posts.comments.CommentManager
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
open class Post(
        val id: Long,
        val createdAt: Long,
        val authorId: Long,
        val feed: String,
        title: String,
        content: String,
        upvotes: Long,
        downvotes: Long
) {
    /**
     * The post's attributes.
     */
    private val attributes = PostAttributes(id)

    /**
     * If the post is hidden.
     */
    var hidden: Boolean
        get() = attributes.getAttribute("hidden", false)
        set(value) = attributes.setAttribute("hidden", value)

    /**
     * If the post is pinned.
     */
    var pinned: Boolean
        get() = attributes.getAttribute("pinned", false)
        set(value) = attributes.setAttribute("pinned", value)

    /**
     * If this post has been previously editied. <- pepega
     */
    var edited: Boolean
        get() = attributes.getAttribute("edited", false)
        set(value) = attributes.setAttribute("edited", value)

    /**
     * If the post is not safe for work.
     */
    var nsfw: Boolean
        get() = attributes.getAttribute("nsfw", false)
        set(value) = attributes.setAttribute("pinned", value)

    /**
     * Change a post's title.
     */
    var title = title
        set(value) {
            Mongo.getClient()
                    .getDatabase("feeds")
                    .getCollection("posts")
                    .updateOne(Filters.eq("id", id), Updates.set("title", value))

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
                    .updateOne(Filters.eq("id", id), Updates.set("content", value))

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