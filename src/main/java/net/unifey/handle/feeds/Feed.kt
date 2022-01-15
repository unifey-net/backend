package net.unifey.handle.feeds

import kotlin.math.ceil
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import net.unifey.handle.feeds.FeedManager.POSTS_PAGE_SIZE
import net.unifey.handle.feeds.responses.UserFeedPermissions
import net.unifey.handle.users.User

@Serializable
data class Feed(
    val id: String,
    val banned: MutableList<Long>,
    val moderators: MutableList<Long>,
) {
    /** TODO */
    val postCount
        get() = runBlocking { FeedManager.getPostCount(id) }

    val pageCount
        get() = ceil(postCount.toDouble() / POSTS_PAGE_SIZE.toDouble()).toLong()

    /** Get [user]'s permissions for this feed. */
    suspend fun getFeedPermissions(user: User): UserFeedPermissions {
        return UserFeedPermissions(
            FeedManager.canViewFeed(this, user.id),
            FeedManager.canPostFeed(this, user.id),
            FeedManager.canCommentFeed(this, user.id)
        )
    }
}
