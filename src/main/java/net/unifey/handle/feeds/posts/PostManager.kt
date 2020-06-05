package net.unifey.handle.feeds.posts

import net.unifey.DatabaseHandler
import net.unifey.handle.NoPermission
import net.unifey.handle.NotFound
import net.unifey.handle.feeds.Feed
import net.unifey.handle.feeds.FeedManager
import net.unifey.util.IdGenerator
import java.util.concurrent.ConcurrentHashMap

object PostManager {
    /**
     * Post cache
     */
    val posts = ConcurrentHashMap<Long, Post>()

    /**
     * Add a post to the database.
     */
    fun createPost(post: Post): Post {
        FeedManager.getFeedPosts(FeedManager.getFeed(post.feed)!!, null).add(post)

        DatabaseHandler.getConnection()
                .prepareStatement("INSERT INTO posts (id, created_at, author_id, content, feed, hidden, title, upvotes, downvotes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
                .apply {
                    setLong(1, post.id)
                    setLong(2, post.createdAt)
                    setLong(3, post.authorId)
                    setString(4, post.content)
                    setString(5, post.feed)
                    setInt(6, if (post.hidden) 1 else 0)
                    setString(7, post.title)
                    setLong(8, post.upvotes)
                    setLong(9, post.downvotes)
                }
                .executeUpdate()

        return post
    }

    /**
     * Add a post to the database.
     */
    fun createPost(feed: Feed, title: String, content: String, author: Long): Post {
        if (!FeedManager.canPostFeed(feed, author))
            throw NoPermission()

        val post = Post(
                IdGenerator.getId(),
                System.currentTimeMillis(),
                author,
                feed.id,
                title,
                content,
                false,
                0,
                0
        )

        return createPost(post)
    }

    /**
     * Get a post by it's [id].
     *
     * @throws PostDoesntExist If the post doesn't exist.
     */
    fun getPost(id: Long): Post {
        if (posts.containsKey(id))
            return posts[id]!!

        val rs = DatabaseHandler.getConnection()
                .prepareStatement("SELECT * FROM posts WHERE id = ?")
                .apply { setLong(1, id) }
                .executeQuery()

        if (rs.next()) {
            val post = Post(
                    rs.getLong("id"),
                    rs.getLong("created_at"),
                    rs.getLong("author_id"),
                    rs.getString("feed"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getInt("hidden") == 1,
                    rs.getLong("upvotes"),
                    rs.getLong("downvotes")
            )

            posts[id] = post

            return posts[id]!!
        } else throw NotFound("post")
    }

    /**
     * Delete a post by it's [id].
     */
    fun deletePost(id: Long) {
        DatabaseHandler.getConnection()
                .prepareStatement("DELETE FROM posts WHERE id = ?")
                .apply { setLong(1, id) }
                .executeUpdate()

        posts.remove(id)
    }
}