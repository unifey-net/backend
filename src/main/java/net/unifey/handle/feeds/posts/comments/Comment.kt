package net.unifey.handle.feeds.posts.comments

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import net.unifey.handle.feeds.posts.Post
import net.unifey.handle.feeds.posts.PostAttributes
import net.unifey.handle.mongo.Mongo

/**
 * A comment. This could be on an actual [Post] or on another [Comment]. This depends on [level].
 *
 * @param parent The parent comment. This could also be [post]
 * @param post The top level post.
 * @param level Level 1 is a comment on the main post, level 2 is a comment on a comment.
 * @param id The ID of the post.
 * @param createdAt When the comment was created.
 * @param authorId The author's ID.
 * @param feed The feed the comment is in.
 * @param content The comment's content.
 * @param upvotes The upvotes.
 * @param downvotes The downvotes.
 */
class Comment(
    val parent: Long,
    val post: Long,
    val level: Int,
    val id: Long,
    val createdAt: Long,
    val authorId: Long,
    val feed: String,
    content: String,
    upvotes: Long,
    downvotes: Long
) {
    /** The comment's attributes. */
    private val attributes = PostAttributes(id, "comments")

    /** If the post is pinned. */
    var pinned: Boolean
        get() = attributes.getAttribute("pinned", false)
        set(value) = attributes.setAttribute("pinned", value)

    /** If the post is hidden. */
    var hidden: Boolean
        get() = attributes.getAttribute("hidden", false)
        set(value) = attributes.setAttribute("hidden", value)

    /** If this comment has been previously edited. */
    var edited: Boolean
        get() = attributes.getAttribute("edited", false)
        set(value) = attributes.setAttribute("edited", value)

    /** Change a post's content */
    var content = content
        set(value) {
            Mongo.getClient()
                .getDatabase("feeds")
                .getCollection("comments")
                .updateOne(Filters.eq("id", id), Updates.set("content", value))

            field = value
        }

    /** A post's upvotes */
    var upvotes = upvotes
        set(value) {
            Mongo.getClient()
                .getDatabase("feeds")
                .getCollection("comments")
                .updateOne(Filters.eq("id", id), Updates.set("vote.upvotes", value))

            field = value
        }

    /** A post's downvotes */
    var downvotes = downvotes
        set(value) {
            Mongo.getClient()
                .getDatabase("feeds")
                .getCollection("comments")
                .updateOne(Filters.eq("id", id), Updates.set("vote.downvotes", value))

            field = value
        }
}
