package net.unifey.handle.feeds.posts.comments

import kotlinx.serialization.Serializable
import net.unifey.handle.feeds.posts.Post

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
 * @param upVotes The upvotes.
 * @param downVotes The downvotes.
 */
@Serializable
class Comment(
    val parent: Long,
    val post: Long,
    val level: Int,
    val id: Long,
    val createdAt: Long,
    val authorId: Long,
    val feed: String,
    val content: String,
    val upVotes: Long,
    val downVotes: Long,
    val pinned: Boolean,
    val hidden: Boolean,
    val edited: Boolean
)
