package net.unifey.handle.feeds

import kotlinx.serialization.Serializable
import net.unifey.handle.feeds.FeedManager.POSTS_PAGE_SIZE
import net.unifey.handle.feeds.responses.UserFeedPermissions
import net.unifey.handle.users.User
import kotlin.math.ceil

@Serializable
data class Feed(
    val id: String,
    val banned: List<Long>,
    val moderators: List<Long>,
) {
    /** TODO */
    suspend fun getPostCount(): Long {
        return FeedManager.getPostCount(id)
    }

    suspend fun getPageCount(): Long {
        return ceil(getPostCount().toDouble() / POSTS_PAGE_SIZE.toDouble()).toLong()
    }

    /** Get [user]'s permissions for this feed. */
    suspend fun getFeedPermissions(user: User): UserFeedPermissions {
        return UserFeedPermissions(
            FeedManager.canViewFeed(this, user.id),
            FeedManager.canPostFeed(this, user.id),
            FeedManager.canCommentFeed(this, user.id)
        )
    }
}
