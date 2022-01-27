package net.unifey.handle.feeds.posts.comments

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import kotlin.math.ceil
import kotlinx.coroutines.flow.map
import net.unifey.handle.LimitReached
import net.unifey.handle.NoPermission
import net.unifey.handle.NotFound
import net.unifey.handle.feeds.FeedManager
import net.unifey.handle.feeds.SortingMethod
import net.unifey.handle.feeds.posts.Post
import net.unifey.handle.feeds.posts.PostManager
import net.unifey.handle.feeds.posts.vote.VoteManager
import net.unifey.handle.mongo.MONGO
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.User
import net.unifey.handle.users.UserManager
import net.unifey.util.IdGenerator
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.aggregate

object CommentManager {
    private const val MAX_THREAD_SIZE = 100
    private const val COMMENT_PAGE_SIZE = 15

    /**
     * Create a comment on [post].
     *
     * @param topPost The top level post that the user is commenting on.
     * @param parent Set this if the user is commenting on a comment. If not, parent is [topPost].
     * @param isComment If it's a post or a comment.
     */
    @Throws(NotFound::class)
    suspend fun createComment(
        topPost: Long,
        parent: Long?,
        feed: String,
        user: User,
        content: String
    ) {
        var level = 1
        var actualParent = topPost

        if (parent != null) {
            val comment = getCommentById(parent)
            actualParent = parent

            if (comment.level >= 2) throw NoPermission()

            level = 2
        } else {
            PostManager.getPost(topPost)
        }

        if (getAmountOfComments(actualParent) >= MAX_THREAD_SIZE) throw LimitReached()

        val comment =
            Comment(
                parent = actualParent,
                post = topPost,
                level = level,
                id = IdGenerator.getId(),
                createdAt = System.currentTimeMillis(),
                authorId = user.id,
                feed = feed,
                content = content,
                upVotes = 0,
                downVotes = 0,
                pinned = false,
                hidden = false,
                edited = false,
            )

        MONGO.getDatabase("feeds").getCollection<Comment>("comments").insertOne(comment)
    }

    /** Delete [comment] */
    suspend fun deleteComment(comment: Comment) {
        MONGO
            .getDatabase("feeds")
            .getCollection<Comment>("commments")
            .deleteOne(and(Comment::id eq comment.id, Comment::parent eq comment.parent))
    }

    /** Get the amount of comments on a [post]. */
    private suspend fun getAmountOfComments(post: Long): Int {
        return MONGO
            .getDatabase("feeds")
            .getCollection<Comment>("comments")
            .countDocuments(Comment::parent eq post)
            .toInt()
    }

    /** Get the [page] of a [CommentData] on a [post]. */
    suspend fun getPostCommentData(
        post: Post,
        page: Int,
        sort: SortingMethod = SortingMethod.NEW,
        user: Long?
    ): CommentData {
        val size = getAmountOfComments(post.id)

        return CommentData(
            size,
            ceil(size.toDouble() / COMMENT_PAGE_SIZE.toDouble()).toInt(),
            getComments(post.id, page, sort, user)
        )
    }

    suspend fun getCommentData(
        comment: Comment,
        page: Int,
        sort: SortingMethod,
        user: Long?
    ): CommentData {
        val size = getAmountOfComments(comment.id)

        return CommentData(
            size,
            ceil(size.toDouble() / COMMENT_PAGE_SIZE.toDouble()).toInt(),
            getComments(comment.id, page, sort, user)
        )
    }

    /** Get comments for [post]. */
    private suspend fun getComments(
        post: Long,
        page: Int,
        sort: SortingMethod,
        user: Long?
    ): MutableList<GetCommentResponse> {
        val startAt = ((page - 1) * COMMENT_PAGE_SIZE)

        val comments =
            Mongo.getClient()
                .getDatabase("feeds")
                .getCollection("comments")
                .find(Filters.eq("parent", post))

        val sorted =
            when (sort) {
                SortingMethod.NEW -> Sorts.descending("createdAt")
                SortingMethod.TOP -> Sorts.descending("vote.upVotes")
                SortingMethod.OLD -> Sorts.ascending("createdAt")
            }

        return MONGO
            .getDatabase("feeds")
            .getCollection<Comment>("comments")
            .aggregate<Comment>(Comment::parent eq post, sort(sorted), skip(startAt))
            .toList()
            .map { comment ->
                GetCommentResponse(
                    comment,
                    if (comment.level == 1) getCommentData(comment, 1, SortingMethod.OLD, user)
                    else null,
                    if (user != null) VoteManager.getCommentVote(comment.id, user) else null,
                    UserManager.getUser(comment.authorId)
                )
            }
            .toMutableList()
    }

    /** Get a comment by it's [id] */
    @Throws(NotFound::class)
    suspend fun getCommentById(id: Long): Comment {
        return MONGO
            .getDatabase("feeds")
            .getCollection<Comment>("comments")
            .findOne(Comment::id eq id)
            ?: throw NotFound("comment")
    }

    /**
     * If [user] can manage [comment]. This means either editing the content or deleting it. This
     * can happen if [user] is the author of the post, or a moderator of the feed.
     */
    suspend fun canManageComment(user: Long, comment: Comment): Boolean {
        return user == comment.authorId ||
            FeedManager.getFeed(comment.feed).moderators.contains(user)
    }

    /** Set [comment]'s [content]. */
    suspend fun setContent(comment: Long, content: String) {
        MONGO
            .getDatabase("feeds")
            .getCollection<Comment>("comments")
            .updateOne(Comment::id eq comment, setValue(Comment::content, content))
    }
}
