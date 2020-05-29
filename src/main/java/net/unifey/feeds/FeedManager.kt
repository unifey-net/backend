package net.unifey.feeds

import net.unifey.DatabaseHandler
import net.unifey.auth.users.User
import net.unifey.feeds.posts.Post
import net.unifey.util.IdGenerator
import org.json.JSONArray

object FeedManager {
    fun getCommunityFeed(): Feed =
            TODO()

    /**
     * A user's feed's ID is "uf_(user ID)"
     */
    fun getUserFeed(user: User): Feed? =
            getFeed("uf_${user.uid}")

    /**
     * Create a feed for [id] user
     */
    fun createFeedForUser(id: Long) {
        DatabaseHandler.getConnection()
                .prepareStatement("INSERT INTO feeds (banned, moderators, id) VALUES (?, ?, ?)")
                .apply {
                    setString(1, "[]")
                    setString(2, "[${id}]")
                    setLong(3, id)
                }
                .executeUpdate()
    }

    /**
     * Get a feed by it's ID.
     */
    fun getFeed(id: String): Feed? {
        val rs = DatabaseHandler.getConnection()
                .prepareStatement("SELECT banned, moderators FROM feeds WHERE id = ?")
                .apply { setString(1, id) }
                .executeQuery()

        return if (rs.next())
            Feed(id, JSONArray(rs.getString("banned")), JSONArray(rs.getString("moderators")))
        else null
    }

    /**
     * If [user] can view [feed].
     */
    fun canViewFeed(feed: Feed, user: Long): Boolean {
        // TODO In the future, check if a user can view a community.

        return true
    }

    /**
     * If [user] can post to [feed].
     */
    fun canPostFeed(feed: Feed, user: Long): Boolean {
        for (i in 0 until feed.banned.length())
            if (feed.banned.getLong(i) == user) return false

        return true
    }

    /**
     * Get [feed]'s posts. If [byUser] is set and they are not able to view the post [canViewFeed], it will throw [CannotViewFeed].
     */
    fun getFeedPosts(feed: Feed, byUser: Long?): MutableList<Post> {
        if (byUser != null && !canViewFeed(feed, byUser))
            throw CannotViewFeed()

        val rs = DatabaseHandler.getConnection()
                .prepareStatement("SELECT id, created_at, author_uid, content, hidden, title FROM posts WHERE feed = ?")
                .apply { setString(1, feed.id) }
                .executeQuery()

        val posts = mutableListOf<Post>()

        while (rs.next()) {
            posts.add(Post(
                    rs.getLong("id"),
                    rs.getLong("created_at"),
                    rs.getLong("author_uid"),
                    rs.getString("title"),
                    rs.getString("content"),
                    feed.id,
                    rs.getInt("hidden") == 1
            ))
        }

        return posts
    }
}