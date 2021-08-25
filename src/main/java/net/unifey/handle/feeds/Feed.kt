package net.unifey.handle.feeds

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import net.unifey.handle.feeds.FeedManager.POSTS_PAGE_SIZE
import net.unifey.handle.feeds.responses.UserFeedPermissions
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.User
import kotlin.math.ceil

class Feed(
        val id: String,
        val banned: MutableList<Long>,
        val moderators: MutableList<Long>,
        val postCount: Long,
        val pageCount: Long = ceil(postCount.toDouble() / POSTS_PAGE_SIZE.toDouble()).toLong()
) {
    /**
     * Update this feeds data to the database
     */
    fun update() {
        Mongo.getClient()
                .getDatabase("feeds")
                .getCollection("feeds")
                .updateOne(Filters.eq("id", id), Updates.combine(Updates.set("banned", banned), Updates.set("moderators", moderators)))
    }

    /**
     * Get [user]'s permissions for this feed.
     */
    fun getFeedPermissions(user: User): UserFeedPermissions {
        return UserFeedPermissions(
            FeedManager.canViewFeed(this, user.id),
            FeedManager.canPostFeed(this, user.id),
            FeedManager.canCommentFeed(this, user.id)
        )
    }
}