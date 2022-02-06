package net.unifey.handle.feeds.posts.vote

import kotlinx.coroutines.runBlocking
import net.unifey.handle.InvalidArguments
import net.unifey.handle.feeds.posts.Post
import net.unifey.handle.feeds.posts.PostManager
import net.unifey.handle.feeds.posts.comments.Comment
import net.unifey.handle.feeds.posts.comments.CommentManager
import net.unifey.handle.mongo.MONGO
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.setValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** There's probably a much better way of doing this. */
object VoteManager {
    val LOGGER: Logger = LoggerFactory.getLogger(this.javaClass)

    private const val UP_VOTE = 1
    private const val DOWN_VOTE = 0
    private const val NO_VOTE = -1

    /** A cache of all user's votes. */
    private val voteCache: MutableList<UserVote> by lazy {
        LOGGER.trace("Loading vote cache...")
        val cache = runBlocking {
            MONGO
                .getDatabase("feeds")
                .getCollection<UserVote>("votes")
                .find()
                .toList()
                .toMutableList()
        }

        LOGGER.trace("Loaded vote cache! Found ${cache.size} votes.")

        cache
    }

    /** Get a [user]'s vote on a [post]. */
    fun getPostVote(post: Long, user: Long): UserVote? =
        voteCache.filter { vote -> vote.type == UserVote.POST }.singleOrNull { vote ->
            vote.id == post && vote.user == user
        }

    fun getCommentVote(comment: Long, user: Long): UserVote? =
        voteCache.filter { vote -> vote.type == UserVote.COMMENT }.singleOrNull { vote ->
            vote.id == comment && vote.user == user
        }

    /** Set a [user]'s vote a [comment]. */
    suspend fun setCommentVote(comment: Long, user: Long, type: Int) {
        if (type != UP_VOTE && type != DOWN_VOTE && type != NO_VOTE) throw InvalidArguments("type")

        val commentObj = CommentManager.getCommentById(comment)
        val currentVote = getCommentVote(comment, user)

        if (currentVote != null) {
            val setVal =
                when (currentVote.vote) {
                    UP_VOTE -> setValue(Comment::upVotes, commentObj.upVotes - 1)
                    DOWN_VOTE -> setValue(Comment::downVotes, commentObj.downVotes - 1)
                    else -> setValue(Comment::upVotes, commentObj.upVotes - 1)
                }

            MONGO
                .getDatabase("feeds")
                .getCollection<Comment>("comments")
                .updateOne(Comment::id eq commentObj.id, setVal)

            voteCache.remove(currentVote)

            MONGO
                .getDatabase("feeds")
                .getCollection<UserVote>("votes")
                .deleteOne(
                    and(
                        UserVote::id eq comment,
                        UserVote::type eq UserVote.COMMENT,
                        UserVote::user eq user
                    )
                )
        }

        if (type != NO_VOTE) {
            val vote = UserVote(type, comment, user, UserVote.COMMENT)

            voteCache.add(vote)

            MONGO.getDatabase("feeds").getCollection<UserVote>("votes").insertOne(vote)

            val setVal =
                when (type) {
                    UP_VOTE -> setValue(Comment::upVotes, commentObj.upVotes + 1)
                    DOWN_VOTE -> setValue(Comment::downVotes, commentObj.downVotes + 1)
                    else -> setValue(Comment::upVotes, commentObj.upVotes)
                }

            MONGO
                .getDatabase("feeds")
                .getCollection<Comment>("comments")
                .updateOne(Comment::id eq commentObj.id, setVal)
        }
    }

    /** Set a [user]'s vote on a [post]. Then update that [post]'s votes. */
    @Throws(InvalidArguments::class)
    suspend fun setPostVote(post: Long, user: Long, type: Int) {
        if (type != UP_VOTE && type != DOWN_VOTE && type != NO_VOTE) throw InvalidArguments("type")

        val postObj = PostManager.getPost(post)
        val currentVote = getPostVote(post, user)

        if (currentVote != null) {
            val setVal =
                when (currentVote.vote) {
                    UP_VOTE -> setValue(Post::upVotes, postObj.upVotes - 1)
                    DOWN_VOTE -> setValue(Post::downVotes, postObj.downVotes - 1)
                    else -> setValue(Post::upVotes, postObj.upVotes - 1)
                }

            voteCache.remove(currentVote)

            MONGO
                .getDatabase("feeds")
                .getCollection<Post>("posts")
                .updateOne(Post::id eq postObj.id, setVal)

            MONGO
                .getDatabase("feeds")
                .getCollection<UserVote>("votes")
                .deleteOne(
                    and(
                        UserVote::id eq postObj.id,
                        UserVote::type eq UserVote.COMMENT,
                        UserVote::user eq user
                    )
                )
        }

        if (type != NO_VOTE) {
            val vote = UserVote(type, post, user, UserVote.POST)
            voteCache.add(vote)

            MONGO.getDatabase("feeds").getCollection<UserVote>("votes").insertOne(vote)

            val setVal =
                when (type) {
                    UP_VOTE -> setValue(Post::upVotes, postObj.upVotes + 1)
                    DOWN_VOTE -> setValue(Post::downVotes, postObj.downVotes + 1)
                    else -> setValue(Post::upVotes, postObj.upVotes)
                }

            MONGO
                .getDatabase("feeds")
                .getCollection<Post>("posts")
                .updateOne(Post::id eq postObj.id, setVal)
        }
    }
}
