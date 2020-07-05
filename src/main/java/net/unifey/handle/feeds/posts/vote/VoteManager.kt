package net.unifey.handle.feeds.posts.vote

import com.mongodb.client.model.Filters
import net.unifey.handle.InvalidArguments
import net.unifey.handle.feeds.posts.PostManager
import net.unifey.handle.mongo.Mongo
import org.bson.Document
import java.util.concurrent.ConcurrentHashMap

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
                            doc.getLong("post"),
                            doc.getLong("user")
                    )
                }
                .toMutableList()
    }

    /**
     * Get a [user]'s vote on a [post].
     */
    fun getVote(post: Long, user: Long): UserVote? =
            voteCache.singleOrNull { vote -> vote.post == post && vote.user == user }

    /**
     * Set a [user]'s vote on a [post]. Then update that [post]'s votes.
     */
    @Throws(InvalidArguments::class)
    fun setVote(post: Long, user: Long, type: Int) {
        if (type != UP_VOTE && type != DOWN_VOTE)
            throw InvalidArguments("type")

        val postObj = PostManager.getPost(post)
        val currentVote = getVote(post, user)

        if (currentVote != null) {
            when (currentVote.vote) {
                UP_VOTE -> postObj.upvotes--
                DOWN_VOTE -> postObj.downvotes--
            }

            voteCache.remove(currentVote)

            Mongo.getClient()
                    .getDatabase("feeds")
                    .getCollection("votes")
                    .deleteOne(Filters.and(Filters.eq("post", post), Filters.eq("user", user)))
        }

        voteCache.add(UserVote(
                type,
                post,
                user
        ))

        Mongo.getClient()
                .getDatabase("feeds")
                .getCollection("votes")
                .insertOne(Document(mapOf(
                        "post" to post,
                        "user" to user,
                        "vote" to type
                )))

        when (type) {
            UP_VOTE -> postObj.upvotes++
            DOWN_VOTE -> postObj.downvotes++
        }
    }
}