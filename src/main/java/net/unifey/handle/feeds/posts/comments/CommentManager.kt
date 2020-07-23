package net.unifey.handle.feeds.posts.comments

import com.mongodb.client.model.Filters
import net.unifey.handle.LimitReached
import net.unifey.handle.NoPermission
import net.unifey.handle.NotFound
import net.unifey.handle.feeds.FeedManager
import net.unifey.handle.feeds.posts.Post
import net.unifey.handle.feeds.posts.PostManager
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.User
import net.unifey.handle.users.UserManager
import net.unifey.util.IdGenerator
import org.bson.Document
import kotlin.math.ceil
object CommentManager {
    private const val MAX_THREAD_SIZE = 100
    private const val COMMENT_PAGE_SIZE = 15

    /**
     * Create a comment on [post].
     *
     * @param post The post/comment to comment on.
     * @param isComment If it's a post or a comment.
     */
    @Throws(NotFound::class)
    fun createComment(post: Long, feed: String, isComment: Boolean, user: User, content: String) {
        var level = 1

        // to ensure it exists
        if (isComment) {
            val comment = getCommentById(post)

            if (comment.level >= 2)
                throw NoPermission()

            level = 2
        } else {
            PostManager.getPost(post)
        }

        if (getAmountOfComments(post) >= MAX_THREAD_SIZE)
            throw LimitReached()

        val comment = Comment(
                post,
                level,
                IdGenerator.getId(),
                System.currentTimeMillis(),
                user.id,
                feed,
                content,
                0,
                0
        )

        Mongo.getClient()
                .getDatabase("feeds")
                .getCollection("comments")
                .insertOne(Document(mapOf(
                        "id" to comment.id,
                        "authorId" to comment.authorId,
                        "level" to comment.level,
                        "feed" to comment.feed,
                        "parent" to comment.parent,
                        "createdAt" to comment.createdAt,
                        "content" to comment.content,
                        "vote" to Document(mapOf(
                                "upvotes" to comment.upvotes,
                                "downvotes" to comment.downvotes
                        )),
                        "attributes" to Document(mapOf(
                                "hidden" to false,
                                "pinned" to false
                        ))
                )))
    }

    /**
     * Delete [comment]
     */
    fun deleteComment(comment: Comment) {
        Mongo.getClient()
                .getDatabase("feeds")
                .getCollection("comments")
                .deleteOne(Filters.and(Filters.eq("id", comment.id), Filters.eq("parent", comment.parent)))
    }

    fun getAmountOfComments(post: Long): Int {
        return Mongo.getClient()
                .getDatabase("feeds")
                .getCollection("comments")
                .find(Filters.eq("parent", post))
                .toList()
                .size
    }

    fun getPostCommentData(post: Post, page: Int): CommentData {
        val size = getAmountOfComments(post.id)

        return CommentData(
                size,
                ceil(size.toDouble() / COMMENT_PAGE_SIZE.toDouble()).toInt(),
                getComments(post.id, page)
        )
    }

    fun getCommentData(comment: Comment, page: Int): CommentData {
        val size = getAmountOfComments(comment.id)

        return CommentData(
                size,
                ceil(size.toDouble() / COMMENT_PAGE_SIZE.toDouble()).toInt(),
                getComments(comment.id, page)
        )
    }

    /**
     * Get comments for [post].
     */
    fun getComments(post: Long, page: Int): MutableList<GetCommentResponse> {
        val startAt = ((page - 1) * COMMENT_PAGE_SIZE)

        return Mongo.getClient()
                .getDatabase("feeds")
                .getCollection("comments")
                .find(Filters.eq("parent", post))
                .skip(startAt)
                .take(COMMENT_PAGE_SIZE)
                .map(CommentManager::getComment)
                .map { comment ->
                    GetCommentResponse(
                            comment,
                            if (comment.level == 1) getCommentData(comment, 1) else null,
                            UserManager.getUser(comment.authorId)
                    )
                }
                .toMutableList()
    }

    /**
     * Get a comment by it's [id]
     */
    @Throws(NotFound::class)
    fun getCommentById(id: Long): Comment {
        val doc = Mongo.getClient()
                .getDatabase("feeds")
                .getCollection("comments")
                .find(Filters.eq("id", id))
                .firstOrNull()
                ?: throw NotFound("comment")

        return getComment(doc)
    }

    /**
     * If [user] can manage [comment]. This means either editing the content or deleting it.
     * This can happen if [user] is the author of the post, or a moderator of the feed.
     */
    fun canManageComment(user: Long, comment: Comment): Boolean {
        return user == comment.authorId || FeedManager.getFeed(comment.feed).moderators.contains(user)
    }

    /**
     * Get a [Comment] from a [doc]
     */
    private fun getComment(doc: Document): Comment {
        val vote = doc["vote"] as Document

        return Comment(
                doc.getLong("parent"),
                doc.getInteger("level"),
                doc.getLong("id"),
                doc.getLong("createdAt"),
                doc.getLong("authorId"),
                doc.getString("feed"),
                doc.getString("content"),
                vote.getLong("upvotes"),
                vote.getLong("downvotes")
        )
    }
}

