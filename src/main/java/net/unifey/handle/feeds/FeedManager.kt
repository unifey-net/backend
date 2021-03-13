package net.unifey.handle.feeds

import com.mongodb.client.FindIterable
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.or
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
    fun canViewFeed(feed: Feed, user: Long?): Boolean {
        if (feed.id.startsWith("cf_")) {
            val community = CommunityManager.getCommunityById(feed.id.removePrefix("cf_").toLongOrNull() ?: throw InvalidArguments("feed id"))

            if (community.viewRole != CommunityRoles.DEFAULT && user == null)
                return false

            return if (user == null)
                CommunityRoles.hasPermission(CommunityRoles.DEFAULT, community.viewRole)
            else
                CommunityRoles.hasPermission(community.getRole(user), community.viewRole)
        }

        return true
    }

    /**
     * If [user] can post to [feed].
     */
    fun canPostFeed(feed: Feed, user: Long?): Boolean {
        if (user == null)
            return false

        return if (feed.id.startsWith("cf")) {
            val community = CommunityManager.getCommunityById(feed.id.removePrefix("cf_").toLong())

            val role = community.getRole(user) ?: CommunityRoles.DEFAULT

            role >= community.postRole && !feed.banned.contains(user)
        } else !feed.banned.contains(user)
    }

    /**
     * If [user] can comment on a post in [feed].
     */
    fun canCommentFeed(feed: Feed, user: Long?): Boolean {
        if (user == null)
            return false

        return if (feed.id.startsWith("cf")) {
            val community = CommunityManager.getCommunityById(feed.id.removePrefix("cf_").toLong())

            val role = community.getRole(user) ?: CommunityRoles.DEFAULT

            role >= community.commentRole && !feed.banned.contains(user)
        } else !feed.banned.contains(user)
    }

    /**
     * Get the trending page.
     */
    fun getTrendingFeedPosts(page: Int): MutableList<Post> {
        return TODO()
    }

    /**
     * Get [feed]'s posts.
     * Each [page] has up to [POSTS_PAGE_SIZE] posts.
     *
     * This assumes you've previously checked for permissions. (see if user can view feed)
     */
    @Throws(NoPermission::class, InvalidArguments::class)
    fun getFeedPosts(feed: Feed, page: Int, method: String): MutableList<Post> {
        if (page > feed.pageCount || 0 >= page) throw InvalidArguments("page")

        val parsedMethod = SortingMethod.values()
                .firstOrNull { sort -> sort.toString().equals(method, true) }
                ?: throw InvalidArguments("sort")

        return getFeedPage(feed, page, parsedMethod)
    }

    /**
     * Get [feeds]'s posts.
     * Each [page] has up to [POSTS_PAGE_SIZE] posts. It remains at the same limit disregarding how many feeds are in the query.
     *
     * This assumes you've previously checked for permissions. (see if user can view feed)
     */
    fun getFeedsPosts(feeds: MutableList<Feed>, page: Int, method: String): MutableList<Post> {
        val pageCount = feeds.asSequence().map { feed -> feed.pageCount }.sum()

        if (page > pageCount || 0 >= page) throw InvalidArguments("page")

        val parsedMethod = SortingMethod.values()
            .firstOrNull { sort -> sort.toString().equals(method, true) }
            ?: throw InvalidArguments("sort")

        return getFeedsPages(feeds, page, parsedMethod)
    }

    const val POSTS_PAGE_SIZE = 50

    /**
     * Get a page out of multiple feeds.
     */
    private fun getFeedsPages(feeds: MutableList<Feed>, page: Int, sortMethod: SortingMethod): MutableList<Post> {
        val startAt = ((page - 1) * POSTS_PAGE_SIZE)
        val filters = feeds.map { feed -> eq("feed", feed.id) }

        val posts = Mongo.getClient()
            .getDatabase("feeds")
            .getCollection("posts")
            .find(or(filters))

        return formFeeds(posts, startAt, sortMethod)
    }

    /**
     * Get a page from a feed.
     */
    private fun getFeedPage(feed: Feed, page: Int, sortMethod: SortingMethod): MutableList<Post> {
        val startAt = ((page - 1) * POSTS_PAGE_SIZE)

        val posts = Mongo.getClient()
                .getDatabase("feeds")
                .getCollection("posts")
                .find(eq("feed", feed.id))

        return formFeeds(posts, startAt, sortMethod)
    }

    /**
     * Form a request of posts into a properly formed list of posts.
     */
    private fun formFeeds(posts: FindIterable<Document>, startAt: Int, sortMethod: SortingMethod): MutableList<Post> {
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
                    doc.getString("feed"),
                    doc.getString("title"),
                    doc.getString("content"),
                    vote.getLong("upvotes"),
                    vote.getLong("downvotes")
                )
            }
            .toMutableList()
    }

    /**
     * Get a post by it's [id]
     */
    fun getPost(id: Long): Post {
        val doc = Mongo.getClient()
                .getDatabase("feeds")
                .getCollection("posts")
                .find(eq("id", id))
                .firstOrNull()
                ?: throw NotFound("post")

        val vote = doc.get("vote", Document::class.java)

        return Post(
                doc.getLong("id"),
                doc.getLong("created_at"),
                doc.getLong("author_id"),
                doc.getString("feed"),
                doc.getString("title"),
                doc.getString("content"),
                vote.getLong("upvotes"),
                vote.getLong("downvotes")
        )
    }
}