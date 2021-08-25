package net.unifey.handle.feeds.posts

import com.mongodb.client.model.Filters
import net.unifey.handle.InvalidArguments
import net.unifey.handle.NoPermission
import net.unifey.handle.NotFound
import net.unifey.handle.communities.Community
import net.unifey.handle.communities.CommunityManager
import net.unifey.handle.feeds.Feed
import net.unifey.handle.feeds.FeedManager
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.notification.NotificationManager
import net.unifey.handle.notification.NotificationManager.postNotification
import net.unifey.handle.users.UserManager
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
                        "title" to post.title,
                        "vote" to Document(mapOf(
                                "downvotes" to post.downvotes,
                                "upvotes" to post.upvotes
                        )),
                        "attributes" to Document(mapOf(
                                "hidden" to false,
                                "pinned" to false,
                                "nsfw" to false
                        ))
                )))

        return post
    }

    /**
     * Add a post to the database.
     */
    @Throws(InvalidArguments::class)
    suspend fun createPost(feed: Feed, title: String, content: String, author: Long): Post {
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
                0,
                0
        )

        sendPostNotification(feed, author)

        return createPost(post)
    }

    /**
     * Send create post notifications.
     */
    private suspend fun sendPostNotification(feed: Feed, author: Long) {
        val name = UserManager.getUser(author).username

        when {
            feed.id.startsWith("uf_") -> {
                val user = feed.id.removePrefix("uf_").toLong()

                postNotification(user, "$name just posted on your profile!")
            }

            feed.id.startsWith("cf_") -> {
                val communityId = feed.id.removePrefix("cf_").toLong()
                val community = CommunityManager.getCommunityById(communityId)
                val notifications = CommunityManager.getNotificationsAsync(communityId).await()

                notifications.forEach { user ->
                    user.postNotification("$name just posted in ${community.name}!")
                }
            }
        }
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

        Mongo.getClient()
                .getDatabase("feeds")
                .getCollection("comments")
                .deleteOne(Filters.eq("post", id))

        posts.remove(id)
    }
}