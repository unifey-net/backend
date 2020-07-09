package net.unifey.handle.feeds

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Sorts
import net.unifey.handle.InvalidArguments
import net.unifey.handle.NoPermission
import net.unifey.handle.NotFound
import net.unifey.handle.communities.Community
import net.unifey.handle.communities.CommunityManager
import net.unifey.handle.communities.CommunityRoles
import net.unifey.handle.feeds.posts.Post
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.User
import org.bson.Document
import java.util.concurrent.ConcurrentHashMap

object FeedManager {
    /**
     * Feed cache
     */
    private val cache = ConcurrentHashMap<String, Feed>()

    /**
     * A communities feed.
     */
    fun getCommunityFeed(community: Community): Feed? =
            getFeed("cf_${community.id}")

    /**
     * A user's feed's ID is "uf_(user ID)"
     */
    fun getUserFeed(user: User): Feed? =
            getFeed("uf_${user.id}")

    /**
     * Create a feed for [id] community. [owner] is the creator.
     */
    fun createFeedForCommunity(id: Long, owner: Long) {
        val feedDocument = Document(mapOf(
                "banned" to arrayListOf<Long>(),
                "moderators" to arrayListOf(owner),
                "id" to "cf_${id}"
        ))

        Mongo.getClient()
                .getDatabase("feeds")
                .getCollection("feeds")
                .insertOne(feedDocument)
    }

    /**
     * Create a feed for [id] user
     */
    fun createFeedForUser(id: Long) {
        val feedDocument = Document(mapOf(
                "banned" to mutableListOf<Long>(),
                "moderators" to mutableListOf(id),
                "id" to "uf_${id}"
        ))

        Mongo.getClient()
                .getDatabase("feeds")
                .getCollection("feeds")
                .insertOne(feedDocument)
    }

    /**
     * Get a feed by it's ID.
     */
    fun getFeed(id: String): Feed {
        if (cache.containsKey(id))
            return cache[id]!!

        val document = Mongo.getClient()
                .getDatabase("feeds")
                .getCollection("feeds")
                .find(eq("id", id))
                .firstOrNull()
                ?: throw NotFound("feed")

        return Feed(
                document.getString("id"),
                document["banned"] as MutableList<Long>,
                document["moderators"] as MutableList<Long>,
                getPostCount(document.getString("id"))
        )
    }

    /**
     * Get the amount of posts in a [feed].
     */
    private fun getPostCount(feed: String): Long {
        return Mongo.getClient()
                .getDatabase("feeds")
                .getCollection("posts")
                .countDocuments(eq("feed", feed))
    }

    /**
     * If [user] can view [feed].
     */
    fun canViewFeed(feed: Feed, user: Long): Boolean {
        if (feed.id.startsWith("cf_")) {
            val community = CommunityManager.getCommunityById(feed.id.removePrefix("cf_").toLongOrNull() ?: throw InvalidArguments("feed id"))

            return community.getRole(user) ?: CommunityRoles.DEFAULT >= community.viewRole
        }

        return true
    }

    /**
     * If [user] can post to [feed].
     */
    fun canPostFeed(feed: Feed, user: Long): Boolean {
        return if (feed.id.startsWith("cf")) {
            val community = CommunityManager.getCommunityById(feed.id.removePrefix("cf_").toLong())

            community.getRole(user) ?: CommunityRoles.DEFAULT >= community.postRole
                    && !feed.banned.contains(user)
        } else !feed.banned.contains(user)
    }

    /**
     * Get the trending page.
     */
    fun getTrendingFeedPosts(page: Int): MutableList<Post> {
        return TODO()
    }

    /**
     * Get [feed]'s posts. If [byUser] is set and they are not able to view the post [canViewFeed], it will throw [CannotViewFeed].
     * Each [page] has up to [POSTS_PAGE_SIZE] posts.
     */
    @Throws(NoPermission::class, InvalidArguments::class)
    fun getFeedPosts(feed: Feed, byUser: Long?, page: Int, method: String): MutableList<Post> {
        when {
            byUser != null && !canViewFeed(feed, byUser) ->
                throw NoPermission()

            page > feed.pageCount || 0 >= page ->
                throw InvalidArguments("page")
        }

        val parsedMethod = SortingMethod.values()
                .firstOrNull { sort -> sort.toString().equals(method, true) }
                ?: throw InvalidArguments("sort")

        return getFeedPage(feed, page, parsedMethod)
    }

    const val POSTS_PAGE_SIZE = 50

    /**
     * Get a page from a feed.
     */
    private fun  getFeedPage(feed: Feed, page: Int, sortMethod: SortingMethod): MutableList<Post> {
        val startAt = ((page - 1) * POSTS_PAGE_SIZE)

        val posts = Mongo.getClient()
                .getDatabase("feeds")
                .getCollection("posts")
                .find(eq("feed", feed.id))

        val sorted = when (sortMethod) {
            SortingMethod.NEW ->
                posts.sort(Sorts.descending("created_at"))

            SortingMethod.TOP ->
                posts.sort(Sorts.descending("vote.upvotes"))

            SortingMethod.OLD ->
                posts.sort(Sorts.ascending("created_at"))
        }

        return sorted
                .skip(startAt)
                .take(POSTS_PAGE_SIZE)
                .map { doc ->
                    val vote = doc.get("vote", Document::class.java)

                    Post(
                            doc.getLong("id"),
                            doc.getLong("created_at"),
                            doc.getLong("author_id"),
                            feed.id,
                            doc.getString("title"),
                            doc.getString("content"),
                            doc.getBoolean("hidden"),
                            vote.getLong("upvotes"),
                            vote.getLong("downvotes")
                    )
                }
                .toMutableList()
    }
}