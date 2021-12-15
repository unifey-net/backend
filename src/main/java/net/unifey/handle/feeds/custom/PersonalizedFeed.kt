package net.unifey.handle.feeds.custom

import net.unifey.handle.communities.CommunityManager
import net.unifey.handle.feeds.Feed
import net.unifey.handle.feeds.FeedManager
import net.unifey.handle.feeds.custom.response.GetLimitedFeedResponse
import net.unifey.handle.feeds.posts.vote.VoteManager
import net.unifey.handle.feeds.responses.GetPostResponse
import net.unifey.handle.users.User
import net.unifey.handle.users.UserManager

object PersonalizedFeed {
    /**
     * Get the page count and post count from [feeds].
     */
    private fun getCounts(feeds: MutableList<Feed>): Pair<Long, Long> {
        return feeds.map { feed -> feed.pageCount }.sum() to feeds.map { feed -> feed.postCount }.sum()
    }

    /**
     * Get [user]'s personalized feed. This is a combination of [user]'s subscribed feeds.
     */
    suspend fun getUsersFeed(user: User, page: Int, method: String): GetLimitedFeedResponse {
        val feeds = user.member.getMembers()
            .map { id -> CommunityManager.getCommunityById(id) }
            .map { community -> community.getFeed() }
            .toMutableList()

        val count = getCounts(feeds)

        return GetLimitedFeedResponse(
            FeedManager.getFeedsPosts(feeds, page, method).map { post ->
                val vote = VoteManager.getPostVote(post.id, user.id)
                GetPostResponse(post, UserManager.getUser(post.authorId), vote)
            }.toMutableList(),
            LimitedFeed(
                count.first,
                count.second
            )
        )
    }
}