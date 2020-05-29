package net.unifey.feeds.posts

import net.unifey.DatabaseHandler
import net.unifey.feeds.CannotPostFeed
import net.unifey.feeds.Feed
import net.unifey.feeds.FeedManager
import net.unifey.util.IdGenerator

object PostManager {
    /**
     * Add a post to the database.
     */
    fun createPost(post: Post): Post {
        DatabaseHandler.getConnection()
                .prepareStatement("INSERT INTO posts (id, created_at, author_uid, content, feed, hidden, title) VALUES (?, ?, ?, ?, ?, ?, ?)")
                .apply {
                    setLong(1, post.id)
                    setLong(2, post.createdAt)
                    setLong(3, post.authorUid)
                    setString(4, post.content)
                    setString(5, post.feed)
                    setInt(6, if (post.hidden) 1 else 0)
                    setString(7, post.title)
                }
                .executeUpdate()

        return post
    }

    /**
     * Add a post to the database.
     */
    fun createPost(feed: Feed, title: String, content: String, author: Long): Post {
        if (!FeedManager.canPostFeed(feed, author))
            throw CannotPostFeed()

        val post = Post(
                IdGenerator.getId(),
                System.currentTimeMillis(),
                author,
                title,
                content,
                feed.id,
                false
        )

        createPost(post)

        return post
    }

    /**
     * Get a post by it's [id].
     */
    fun getPost(id: Long): Post? {
        val rs = DatabaseHandler.getConnection()
                .prepareStatement("SELECT * FROM posts WHERE id = ?")
                .apply { setLong(1, id) }
                .executeQuery()

        if (rs.next())
            return Post(
                    rs.getLong("id"),
                    rs.getLong("created_at"),
                    rs.getLong("author_uid"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getString("feed"),
                    rs.getInt("hidden") == 1
            )
        else return null
    }

    /**
     * Delete a post by it's [id].
     */
    fun deletePost(id: Long) {
        DatabaseHandler.getConnection()
                .prepareStatement("DELETE FROM posts WHERE id = ?")
                .apply { setLong(1, id) }
                .executeUpdate()
    }
}