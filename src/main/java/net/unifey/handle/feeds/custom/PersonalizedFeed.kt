package net.unifey.handle.feeds.custom

import net.unifey.handle.communities.CommunityManager
import net.unifey.handle.feeds.Feed
import net.unifey.handle.feeds.FeedManager
import net.unifey.handle.feeds.custom.response.GetLimitedFeedResponse
import net.unifey.handle.feeds.posts.vote.VoteManager
import net.unifey.handle.feeds.responses.GetPostResponse
import net.unifey.handle.users.User
import net.unifey.handle.users.UserManager
import net.unifey.handle.users.member.MemberManager.getMember

object PersonalizedFeed {
    /** Get the page count and post count from [feeds]. */
    private suspend fun getCounts(feeds: MutableList<Feed>): Pair<Long, Long> {
        return feeds.sumOf { feed -> feed.getPageCount() } to
            feeds.sumOf { feed -> feed.getPostCount() }
    }

    /** Get [user]'s personalized feed. This is a combination of [user]'s subscribed feeds. */
    suspend fun getUsersFeed(user: User, page: Int, method: String): GetLimitedFeedResponse {
        val feeds =
            user.getMember()
                .member
                .map { id -> CommunityManager.getCommunityById(id) }
                .map { community -> community.getFeed() }
                .toMutableList()

        val count = getCounts(feeds)

        return GetLimitedFeedResponse(
            FeedManager.getFeedsPosts(feeds, page, method)
                .map { post ->
                    val vote = VoteManager.getPostVote(post.id, user.id)
                    GetPostResponse(post, UserManager.getUser(post.authorId), vote)
                }
                .toMutableList(),
            LimitedFeed(count.first, count.second)
        )
    }
}
