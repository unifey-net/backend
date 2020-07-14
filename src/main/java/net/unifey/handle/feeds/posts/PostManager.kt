package net.unifey.handle.feeds.posts

import com.mongodb.client.model.Filters
import net.unifey.handle.InvalidArguments
import net.unifey.handle.NoPermission
import net.unifey.handle.NotFound
import net.unifey.handle.feeds.Feed
import net.unifey.handle.feeds.FeedManager
import net.unifey.handle.mongo.Mongo
import net.unifey.util.IdGenerator
import net.unifey.util.cleanInput
import org.bson.Document
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
        Mongo.getClient()
                .getDatabase("feeds")
                .getCollection("posts")
                .insertOne(Document(mapOf(
                        "id" to post.id,
                        "created_at" to post.createdAt,
                        "author_id" to post.authorId,
                        "content" to post.content,
                        "feed" to post.feed,
                        "hidden" to post.hidden,
                        "title" to post.title,
                        "vote" to Document(mapOf(
                                "downvotes" to post.downvotes,
                                "upvotes" to post.upvotes
                        ))
                )))

        return post
    }

    /**
     * Add a post to the database.
     */
    @Throws(InvalidArguments::class)
    fun createPost(feed: Feed, title: String, content: String, author: Long): Post {
        if (!FeedManager.canPostFeed(feed, author))
            throw NoPermission()

        val parsedTitle = cleanInput(title)
        val parsedContent = cleanInput(content)

        if (parsedContent.isBlank() || parsedTitle.isBlank())
            throw InvalidArguments("title", "content")

        val post = Post(
                IdGenerator.getId(),
                System.currentTimeMillis(),
                author,
                feed.id,
                parsedTitle,
                parsedContent,
                false,
                0,
                0
        )

        return createPost(post)
    }

    /**
     * Get a post by it's [id].
     */
    @Throws(NotFound::class)
    fun getPost(id: Long): Post {
        if (posts.containsKey(id))
            return posts[id]!!

        val doc = Mongo.getClient()
                .getDatabase("feeds")
                .getCollection("posts")
                .find(Filters.eq("id", id))
                .singleOrNull()

        if (doc != null) {
            val vote = doc.get("vote", Document::class.java)

            val post = Post(
                    doc.getLong("id"),
                    doc.getLong("created_at"),
                    doc.getLong("author_id"),
                    doc.getString("feed"),
                    doc.getString("title"),
                    doc.getString("content"),
                    doc.getBoolean("hidden"),
                    vote.getLong("upvotes"),
                    vote.getLong("downvotes")
            )

            posts[id] = post

            return posts[id]!!
        } else throw NotFound("post")
    }

    /**
     * Delete a post by it's [id].
     */
    fun deletePost(id: Long) {
        Mongo.getClient()
                .getDatabase("feeds")
                .getCollection("posts")
                .deleteOne(Filters.eq("id", id))

        posts.remove(id)
    }
}