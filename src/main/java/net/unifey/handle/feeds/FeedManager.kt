package net.unifey.handle.feeds

import com.fasterxml.jackson.databind.ObjectMapper
import javafx.geometry.Pos
import net.unifey.DatabaseHandler
import net.unifey.handle.users.User
import net.unifey.handle.feeds.posts.Post
import org.json.JSONArray
import java.util.concurrent.ConcurrentHashMap

object FeedManager {
    /**
     * Feed cache
     */
    private val cache = ConcurrentHashMap<String, Feed>()

    /**
     * A feed and it's posts.
     */
    private val feedCache = ConcurrentHashMap<Feed, MutableList<Post>>()

    /**
     * A communities feed.
     */
    fun getCommunityFeed(): Feed? =
            TODO()

    /**
     * A user's feed's ID is "uf_(user ID)"
     */
    fun getUserFeed(user: User): Feed? =
            getFeed("uf_${user.id}")

    /**
     * Create a feed for [id] user
     */
    fun createFeedForUser(id: Long) {
        DatabaseHandler.getConnection()
                .prepareStatement("INSERT INTO feeds (banned, moderators, id) VALUES (?, ?, ?)")
                .apply {
                    setString(1, "[]")
                    setString(2, "[${id}]")
                    setString(3, "uf_${id}")
                }
                .executeUpdate()
    }

    /**
     * Get a feed by it's ID.
     */
    fun getFeed(id: String): Feed? {
        if (cache.containsKey(id))
            return cache[id]!!

        val rs = DatabaseHandler.getConnection()
                .prepareStatement("SELECT banned, moderators FROM feeds WHERE id = ?")
                .apply { setString(1, id) }
                .executeQuery()

        val mapper = ObjectMapper()

        return if (rs.next())
            Feed(
                    id,
                    mapper.readValue(
                            rs.getString("banned"),
                            mapper.typeFactory.constructCollectionType(MutableList::class.java, Long::class.java)
                    ),
                    mapper.readValue(
                            rs.getString("moderators"),
                            mapper.typeFactory.constructCollectionType(MutableList::class.java, Long::class.java)
                    )
            )
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
    fun canPostFeed(feed: Feed, user: Long): Boolean =
            !feed.banned.contains(user)

    /**
     * Get [feed]'s posts. If [byUser] is set and they are not able to view the post [canViewFeed], it will throw [CannotViewFeed].
     */
    fun getFeedPosts(feed: Feed, byUser: Long?): MutableList<Post> {
        if (byUser != null && !canViewFeed(feed, byUser))
            throw CannotViewFeed()

        if (feedCache.containsKey(feed))
            return feedCache[feed]!!

        val rs = DatabaseHandler.getConnection()
                .prepareStatement("SELECT * FROM posts WHERE feed = ?")
                .apply { setString(1, feed.id) }
                .executeQuery()

        val posts = mutableListOf<Post>()

        while (rs.next()) {
            posts.add(Post(
                    rs.getLong("id"),
                    rs.getLong("created_at"),
                    rs.getLong("author_id"),
                    feed.id,
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getInt("hidden") == 1,
                    rs.getLong("upvotes"),
                    rs.getLong("downvotes")
            ))
        }

        feedCache[feed] = posts

        return feedCache[feed]!!
    }
}