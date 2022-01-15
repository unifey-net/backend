package net.unifey.handle.feeds.posts

import net.unifey.handle.InvalidArguments
import net.unifey.handle.NotFound
import net.unifey.handle.communities.CommunityManager
import net.unifey.handle.feeds.Feed
import net.unifey.handle.feeds.posts.comments.Comment
import net.unifey.handle.mongo.MONGO
import net.unifey.handle.notification.NotificationManager.postNotification
import net.unifey.handle.users.UserManager
import net.unifey.util.IdGenerator
import net.unifey.util.cleanInput
import org.litote.kmongo.eq
import org.litote.kmongo.setValue

object PostManager {
    /** Add a post to the database. */
    suspend fun createPost(post: Post): Post {
        MONGO.getDatabase("feeds").getCollection<Post>("posts").insertOne(post)

        return post
    }

    /** Add a post to the database. */
    @Throws(InvalidArguments::class)
    suspend fun createPost(feed: Feed, title: String, content: String, author: Long): Post {
        val parsedTitle = cleanInput(title)
        val parsedContent = cleanInput(content)

        if (parsedContent.isBlank() || parsedTitle.isBlank())
            throw InvalidArguments("title", "content")

        val post =
            Post(
                id = IdGenerator.getId(),
                createdAt = System.currentTimeMillis(),
                authorId = author,
                feed = feed.id,
                title = parsedTitle,
                content = parsedContent,
                upVotes = 0,
                downVotes = 0,
                hidden = false,
                pinned = false,
                edited = false,
                nsfw = false
            )

        sendPostNotification(feed, author)

        return createPost(post)
    }

    /** Send create post notifications. */
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
                val notifications = CommunityManager.getNotifications(communityId)

                notifications.forEach { user ->
                    user.postNotification("$name just posted in ${community.name}!")
                }
            }
        }
    }

    /** Get a post by it's [id]. */
    @Throws(NotFound::class)
    suspend fun getPost(id: Long): Post {
        return MONGO.getDatabase("feeds").getCollection<Post>("posts").findOne(Post::id eq id)
            ?: throw NotFound("post")
    }

    /** Delete a post by it's [id]. */
    suspend fun deletePost(id: Long) {
        MONGO.getDatabase("feeds").getCollection<Post>("posts").deleteOne(Post::id eq id)

        MONGO
            .getDatabase("feeds")
            .getCollection<Comment>("comments")
            .deleteMany(Comment::post eq id)
    }

    /** Set the [post]'s [content]. */
    suspend fun setContent(post: Long, content: String) {
        MONGO
            .getDatabase("feeds")
            .getCollection<Comment>("posts")
            .updateOne(Post::id eq post, setValue(Post::content, content))
    }

    /** Set the [post]'s [title]. */
    suspend fun setTitle(post: Long, title: String) {
        MONGO
            .getDatabase("feeds")
            .getCollection<Post>("posts")
            .updateOne(Post::id eq post, setValue(Post::title, title))
    }

    /** Set if the [post] is [edited]. */
    suspend fun setEdited(post: Long, edited: Boolean) {
        MONGO
            .getDatabase("feeds")
            .getCollection<Post>()
            .updateOne(Post::id eq post, setValue(Post::edited, edited))
    }
}
