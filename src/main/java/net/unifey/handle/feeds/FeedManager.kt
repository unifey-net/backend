package net.unifey.handle.feeds

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.or
import com.mongodb.client.model.Sorts
import net.unifey.handle.InvalidArguments
import net.unifey.handle.NoContent
import net.unifey.handle.NoPermission
import net.unifey.handle.NotFound
import net.unifey.handle.communities.Community
import net.unifey.handle.communities.CommunityManager
import net.unifey.handle.communities.CommunityRoles
import net.unifey.handle.communities.getRole
import net.unifey.handle.feeds.posts.Post
import net.unifey.handle.mongo.MONGO
import net.unifey.handle.users.User
import org.litote.kmongo.coroutine.CoroutineFindPublisher
import org.litote.kmongo.eq
import org.litote.kmongo.pull
import org.litote.kmongo.push
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object FeedManager {
    val LOGGER: Logger = LoggerFactory.getLogger(this.javaClass)

    /** A communities feed. */
    @Throws(NotFound::class)
    suspend fun getCommunityFeed(community: Community): Feed = getFeed("cf_${community.id}")

    /** A user's feed's ID is "uf_(user ID)" */
    @Throws(NotFound::class) suspend fun getUserFeed(user: User): Feed = getFeed("uf_${user.id}")

    /** Create a feed for [id] community. [owner] is the creator. */
    suspend fun createFeedForCommunity(id: Long, owner: Long) {
        LOGGER.trace("CREATE COMMUNITY FEED: $id ($owner) -> cf_${id}")
        MONGO
            .getDatabase("feeds")
            .getCollection<Feed>("feeds")
            .insertOne(Feed("cf_${id}", listOf(), listOf(owner)))
    }

    /** Create a feed for [id] user */
    suspend fun createFeedForUser(id: Long) {
        LOGGER.trace("CREATE USER FEED: $id -> uf_${id}")

        MONGO
            .getDatabase("feeds")
            .getCollection<Feed>("feeds")
            .insertOne(Feed("uf_${id}", listOf(), listOf(id)))
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
    fun getTrendingFeedPosts(page: Int): MutableList<Post> = TODO("$page")

    /**
     * Get [feed]'s posts. Each [page] has up to [POSTS_PAGE_SIZE] posts.
     *
     * This assumes you've previously checked for permissions. (see if user can view feed)
     */
    @Throws(NoPermission::class, InvalidArguments::class, NoContent::class)
    suspend fun getFeedPosts(feed: Feed, page: Int, method: String): MutableList<Post> {
        val pageCount = feed.getPageCount()

        when {
            pageCount == 0L -> throw NoContent()
            page > pageCount || 0 >= page -> throw InvalidArguments("page")
        }

        val parsedMethod =
            SortingMethod.values().firstOrNull { sort -> sort.toString().equals(method, true) }
                ?: throw InvalidArguments("sort")

        LOGGER.trace("FEED POSTS: ${feed.id} ($method) -> $page")

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
        val pageCount = feeds.sumOf { feed -> feed.getPageCount() }

        if (page > pageCount || 0 >= page) throw InvalidArguments("page")

        val parsedMethod =
            SortingMethod.values().firstOrNull { sort -> sort.toString().equals(method, true) }
                ?: throw InvalidArguments("sort")

        LOGGER.trace("FEEDS POSTS: ${feeds.joinToString { feed -> feed.id }} ($method) -> $page")

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
                SortingMethod.NEW -> Sorts.descending("createdAt")
                SortingMethod.TOP -> Sorts.descending("vote.upvotes")
                SortingMethod.OLD -> Sorts.ascending("createdAt")
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
        LOGGER.trace("FEED ADD MODERATOR: $feed ->+ $moderator")

        MONGO
            .getDatabase("feeds")
            .getCollection<Feed>("feeds")
            .updateOne(Feed::id eq feed, push(Feed::moderators, moderator))
    }

    /** Remove [moderator] from [feed] */
    suspend fun removeModerator(feed: String, moderator: Long) {
        LOGGER.trace("FEED REMOVE MODERATOR: $feed ->- $moderator")

        MONGO
            .getDatabase("feeds")
            .getCollection<Feed>("feeds")
            .updateOne(Feed::id eq feed, pull(Feed::moderators, moderator))
    }
}
