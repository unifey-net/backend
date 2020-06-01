package net.unifey.handle.feeds.posts

import java.util.concurrent.ConcurrentHashMap

object VoteManager {
    const val UPVOTE = 1
    const val DOWNVOTE = 0

    /**
     * Key: User ID
     * Value: Pair of a post to either [UPVOTE] or [DOWNVOTE]
     */
    val voteCache = ConcurrentHashMap<Long, Pair<Long, Int>>()

    /**
     *
     */
    fun getVote(post: Long, user: Long): Int =
            TODO()

}