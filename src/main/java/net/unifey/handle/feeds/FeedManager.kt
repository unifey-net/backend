package net.unifey.handle.feeds

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.or
import com.mongodb.client.model.Sorts
import net.unifey.handle.InvalidArguments
import net.unifey.handle.NoPermission
import net.unifey.handle.NotFound
import net.unifey.handle.communities.Community
import net.unifey.handle.communities.CommunityManager
import net.unifey.handle.communities.CommunityRoles
import net.unifey.handle.communities.getRole
import net.unifey.handle.feeds.posts.Post
import net.unifey.handle.mongo.MONGO
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.User
import org.bson.Document
import org.litote.kmongo.coroutine.CoroutineFindPublisher
import org.litote.kmongo.eq
import org.litote.kmongo.pull
import org.litote.kmongo.push

object FeedManager {
    /** A communities feed. */
    @Throws(NotFound::class)
    suspend fun getCommunityFeed(community: Community): Feed = getFeed("cf_${community.id}")

    /** A user's feed's ID is "uf_(user ID)" */
    @Throws(NotFound::class) suspend fun getUserFeed(user: User): Feed = getFeed("uf_${user.id}")

    /** Create a feed for [id] community. [owner] is the creator. */
    suspend fun createFeedForCommunity(id: Long, owner: Long) {
        MONGO
            .getDatabase("feeds")
            .getCollection<Feed>("feeds")
            .insertOne(Feed("cf_${id}", mutableListOf(), mutableListOf(owner)))
    }

    /** Create a feed for [id] user */
    fun createFeedForUser(id: Long) {
        val feedDocument =
            Document(
                mapOf(
                    "banned" to mutableListOf<Long>(),
                    "moderators" to mutableListOf(id),
                    "id" to "uf_${id}"
                )
            )

        Mongo.getClient().getDatabase("feeds").getCollection("feeds").insertOne(feedDocument)
    }

    /** Get a feed by it's ID. */
    @Throws(NotFound::class)
    suspend fun getFeed(id: String): Feed {
        return MONGO.getDatabase("feeds").getCollection<Feed>("feeds").findOne(Feed::id eq id)
            ?: throw NotFound("feed")
    }

    /** Get the amount of posts in a [feed]. */
    suspend fun getPostCount(feed: String): Long {
        return MONGO
            .getDatabase("feeds")
            .getCollection<Post>("posts")
            .countDocuments(Post::feed eq feed)
    }

    /** If [user] can view [feed]. */
    suspend fun canViewFeed(feed: Feed, user: Long?): Boolean {
        if (feed.id.startsWith("cf_")) {
            val community =
                CommunityManager.getCommunityById(
                    feed.id.removePrefix("cf_").toLongOrNull() ?: throw InvalidArguments("feed id")
                )

            if (community.permissions.viewRole != CommunityRoles.DEFAULT && user == null)
                return false

            return if (user == null)
                CommunityRoles.hasPermission(CommunityRoles.DEFAULT, community.permissions.viewRole)
            else
                CommunityRoles.hasPermission(
                    community.getRole(user),
                    community.permissions.viewRole
                )
        }

        return true
    }

    /** If [user] can post to [feed]. */
    suspend fun canPostFeed(feed: Feed, user: Long?): Boolean {
        if (user == null) return false

        return if (feed.id.startsWith("cf")) {
            val community = CommunityManager.getCommunityById(feed.id.removePrefix("cf_").toLong())

            val role = community.getRole(user)

            role >= community.permissions.postRole && !feed.banned.contains(user)
        } else !feed.banned.contains(user)
    }

    /** If [user] can comment on a post in [feed]. */
    suspend fun canCommentFeed(feed: Feed, user: Long?): Boolean {
        if (user == null) return false

        return if (feed.id.startsWith("cf")) {
            val community = CommunityManager.getCommunityById(feed.id.removePrefix("cf_").toLong())

            val role = community.getRole(user)

            role >= community.permissions.commentRole && !feed.banned.contains(user)
        } else !feed.banned.contains(user)
    }

    /** Get the trending page. */
    fun getTrendingFeedPosts(page: Int): MutableList<Post> {
        return TODO()
    }

    /**
     * Get [feed]'s posts. Each [page] has up to [POSTS_PAGE_SIZE] posts.
     *
     * This assumes you've previously checked for permissions. (see if user can view feed)
     */
    @Throws(NoPermission::class, InvalidArguments::class)
    suspend fun getFeedPosts(feed: Feed, page: Int, method: String): MutableList<Post> {
        if (page > feed.pageCount || 0 >= page) throw InvalidArguments("page")

        val parsedMethod =
            SortingMethod.values().firstOrNull { sort -> sort.toString().equals(method, true) }
                ?: throw InvalidArguments("sort")

        return getFeedPage(feed, page, parsedMethod)
    }

    /**
     * Get [feeds]'s posts. Each [page] has up to [POSTS_PAGE_SIZE] posts. It remains at the same
     * limit disregarding how many feeds are in the query.
     *
     * This assumes you've previously checked for permissions. (see if user can view feed)
     */
    suspend fun getFeedsPosts(
        feeds: MutableList<Feed>,
        page: Int,
        method: String
    ): MutableList<Post> {
        val pageCount = feeds.asSequence().map { feed -> feed.pageCount }.sum()

        if (page > pageCount || 0 >= page) throw InvalidArguments("page")

        val parsedMethod =
            SortingMethod.values().firstOrNull { sort -> sort.toString().equals(method, true) }
                ?: throw InvalidArguments("sort")

        return getFeedsPages(feeds, page, parsedMethod)
    }

    const val POSTS_PAGE_SIZE = 50

    /** Get a page out of multiple feeds. */
    private suspend fun getFeedsPages(
        feeds: MutableList<Feed>,
        page: Int,
        sortMethod: SortingMethod
    ): MutableList<Post> {
        val startAt = ((page - 1) * POSTS_PAGE_SIZE)
        val filters = feeds.map { feed -> Post::feed eq feed.id }

        val posts = MONGO.getDatabase("feeds").getCollection<Post>("posts").find(or(filters))

        return formFeeds(posts, startAt, sortMethod)
    }

    /** Get a page from a feed. */
    private suspend fun getFeedPage(
        feed: Feed,
        page: Int,
        sortMethod: SortingMethod
    ): MutableList<Post> {
        val startAt = ((page - 1) * POSTS_PAGE_SIZE)

        val posts =
            MONGO.getDatabase("feeds").getCollection<Post>("posts").find(Post::feed eq feed.id)

        return formFeeds(posts, startAt, sortMethod)
    }

    /** Form a request of posts into a properly formed list of posts. */
    private suspend fun formFeeds(
        posts: CoroutineFindPublisher<Post>,
        startAt: Int,
        sortMethod: SortingMethod
    ): MutableList<Post> {
        val sort =
            when (sortMethod) {
                SortingMethod.NEW -> Sorts.descending("created_at")
                SortingMethod.TOP -> Sorts.descending("vote.upvotes")
                SortingMethod.OLD -> Sorts.ascending("created_at")
            }

        return posts.sort(sort).skip(startAt).limit(POSTS_PAGE_SIZE).toList().toMutableList()
    }

    /** Get a post by it's [id] */
    @Throws(NotFound::class)
    suspend fun getPost(id: Long): Post {
        return MONGO.getDatabase("feeds").getCollection<Post>("posts").findOne(Post::id eq id)
            ?: throw NotFound("post")
    }

    /** Add [moderator] to [feed] */
    suspend fun addModerator(feed: String, moderator: Long) {
        MONGO
            .getDatabase("feeds")
            .getCollection<Feed>("feeds")
            .updateOne(Feed::id eq feed, push(Feed::moderators, moderator))
    }

    /** Remove [moderator] from [feed] */
    suspend fun removeModerator(feed: String, moderator: Long) {
        MONGO
            .getDatabase("feeds")
            .getCollection<Feed>("feeds")
            .updateOne(Feed::id eq feed, pull(Feed::moderators, moderator))
    }
}
