package net.unifey.handle.feeds.posts.vote

import com.mongodb.client.model.Filters
import net.unifey.handle.InvalidArguments
import net.unifey.handle.feeds.posts.PostManager
import net.unifey.handle.feeds.posts.comments.CommentManager
import net.unifey.handle.mongo.Mongo
import org.bson.Document

/**
 * There's probably a much better way of doing this.
 */
object VoteManager {
    private const val UP_VOTE = 1
    private const val DOWN_VOTE = 0
    private const val NO_VOTE = -1

    /**
     * A cache of all user's votes.
     */
    private val voteCache: MutableList<UserVote> by lazy {
        Mongo.getClient()
                .getDatabase("feeds")
                .getCollection("votes")
                .find()
                .map { doc ->
                    UserVote(
                            doc.getInteger("vote"),
                            doc.getLong("id"),
                            doc.getLong("user"),
                            doc.getInteger("type")
                    )
                }
                .toMutableList()
    }

    /**
     * Get a [user]'s vote on a [post].
     */
    fun getPostVote(post: Long, user: Long): UserVote? =
            voteCache
                    .filter { vote -> vote.type == UserVote.POST }
                    .singleOrNull { vote -> vote.id == post && vote.user == user }

    fun getCommentVote(comment: Long, user: Long): UserVote? =
            voteCache
                    .filter { vote -> vote.type == UserVote.COMMENT }
                    .singleOrNull { vote -> vote.id == comment && vote.user == user }

    /**
     * Set a [user]'s vote a [comment].
     */
    fun setCommentVote(comment: Long, user: Long, type: Int) {
        if (type != UP_VOTE && type != DOWN_VOTE && type != NO_VOTE)
            throw InvalidArguments("type")

        val commentObj = CommentManager.getCommentById(comment)
        val currentVote = getCommentVote(comment, user)

        if (currentVote != null) {
            when (currentVote.vote) {
                UP_VOTE -> commentObj.upvotes--
                DOWN_VOTE -> commentObj.downvotes--
            }

            voteCache.remove(currentVote)

            Mongo.getClient()
                    .getDatabase("feeds")
                    .getCollection("votes")
                    .deleteOne(Filters.and(
                            Filters.eq("id", comment),
                            Filters.eq("type", UserVote.COMMENT),
                            Filters.eq("user", user))
                    )
        }

        if (type != NO_VOTE) {
            voteCache.add(UserVote(
                    type,
                    comment,
                    user,
                    UserVote.COMMENT
            ))

            Mongo.getClient()
                    .getDatabase("feeds")
                    .getCollection("votes")
                    .insertOne(Document(mapOf(
                            "id" to comment,
                            "user" to user,
                            "vote" to type,
                            "type" to UserVote.COMMENT
                    )))

            when (type) {
                UP_VOTE -> commentObj.upvotes++
                DOWN_VOTE -> commentObj.downvotes++
            }
        }
    }

    /**
     * Set a [user]'s vote on a [post]. Then update that [post]'s votes.
     */
    @Throws(InvalidArguments::class)
    fun setPostVote(post: Long, user: Long, type: Int) {
        if (type != UP_VOTE && type != DOWN_VOTE && type != NO_VOTE)
            throw InvalidArguments("type")

        val postObj = PostManager.getPost(post)
        val currentVote = getPostVote(post, user)

        if (currentVote != null) {
            when (currentVote.vote) {
                UP_VOTE -> postObj.upvotes--
                DOWN_VOTE -> postObj.downvotes--
            }

            voteCache.remove(currentVote)

            Mongo.getClient()
                    .getDatabase("feeds")
                    .getCollection("votes")
                    .deleteOne(Filters.and(
                            Filters.eq("id", post),
                            Filters.eq("user", user),
                            Filters.eq("type", UserVote.POST)
                    ))
        }

        if (type != NO_VOTE) {
            voteCache.add(UserVote(
                    type,
                    post,
                    user,
                    UserVote.POST
            ))

            Mongo.getClient()
                    .getDatabase("feeds")
                    .getCollection("votes")
                    .insertOne(Document(mapOf(
                            "id" to post,
                            "user" to user,
                            "vote" to type,
                            "type" to UserVote.POST
                    )))

            when (type) {
                UP_VOTE -> postObj.upvotes++
                DOWN_VOTE -> postObj.downvotes++
            }
        }
    }
}