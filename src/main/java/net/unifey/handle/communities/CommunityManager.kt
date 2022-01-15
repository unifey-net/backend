package net.unifey.handle.communities

import com.mongodb.client.model.Filters.eq
import dev.ajkneisl.lib.util.getAge
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import net.unifey.handle.InvalidArguments
import net.unifey.handle.NotFound
import net.unifey.handle.feeds.FeedManager
import net.unifey.handle.mongo.MONGO
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.User
import net.unifey.handle.users.UserManager
import net.unifey.handle.users.member.Member
import net.unifey.handle.users.member.MemberManager.joinCommunity
import net.unifey.util.IdGenerator
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.aggregate
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** Manage [Community]s */
object CommunityManager {
    val LOGGER: Logger = LoggerFactory.getLogger(this.javaClass)

    /**
     * Handle [user] leaving [id]. This removes their role from the data set. This doesn't affect
     * their current posts.
     */
    suspend fun userLeave(id: Long, user: Long) {
        getCommunityById(id).removeRole(user)
    }

    /** Delete a community by it's [id] */
    fun deleteCommunity(id: Long) {
        Mongo.getClient()
            .getDatabase("communities")
            .getCollection("communities")
            .deleteOne(eq("id", id))
    }

    /** Get a [Community] by [name]. */
    @Throws(NotFound::class)
    suspend fun getCommunityByName(name: String): Community {
        return MONGO
            .getDatabase("communities")
            .getCollection<Community>("communities")
            .find()
            .toList()
            .singleOrNull { community -> community.name.equals(name, true) }
            ?: throw NotFound("community")
    }

    /** Get a [Community] by [id]. */
    @Throws(NotFound::class)
    suspend fun getCommunityById(id: Long): Community {
        return MONGO
            .getDatabase("communities")
            .getCollection<Community>("communities")
            .findOne(Community::id eq id)
            ?: throw NotFound("community")
    }

    /** Get a page of communities. */
    suspend fun getCommunities(page: Int): List<Community> {
        val size = getCommunitiesSize()

        // there's 15 communities to a page.
        val pageCount = ceil(size.toDouble() / 15.0).toInt()

        if (page > pageCount || 0 >= page) throw InvalidArguments("page")

        // the amount of documents to skip to get to the proper page.
        val skip = if (page != 1) (page - 1) * 15 else 0

        LOGGER.trace("Loading communities ($page): ($size -> $skip)")

        return MONGO
            .getDatabase("communities")
            .getCollection<Community>("communities")
            .aggregate<Community>(skip(skip), limit(15))
            .toList()
    }

    /** Get the amount of communities */
    private suspend fun getCommunitiesSize(): Long =
        MONGO.getDatabase("communities").getCollection<Community>("communities").countDocuments()

    /**
     * If [id] can create a community
     *
     * They must be verified and have their account for over 14 days.
     */
    suspend fun canCreate(id: Long): Boolean {
        val user = UserManager.getUser(id)

        return user.verified &&
            user.createdAt.getAge() >= TimeUnit.DAYS.toMillis(14) &&
            !hasCreatedCommunityBefore(id)
    }

    /** if [id] has made a community before */
    private suspend fun hasCreatedCommunityBefore(id: Long): Boolean {
        return MONGO
            .getDatabase("communities")
            .getCollection<Community>("communities")
            .findOne(eq("roles.${id}", 4)) != null
    }

    /** Create a community. */
    suspend fun createCommunity(owner: Long, name: String, desc: String): Community {
        val roles = hashMapOf(owner to CommunityRoles.OWNER)

        val community =
            Community(
                id = IdGenerator.getId(),
                createdAt = System.currentTimeMillis(),
                permissions =
                    CommunityPermissions(
                        CommunityRoles.MEMBER,
                        CommunityRoles.DEFAULT,
                        CommunityRoles.MEMBER
                    ),
                name = name,
                description = desc,
                rules = mutableListOf(),
                roles = roles
            )

        MONGO
            .getDatabase("communities")
            .getCollection<Community>("communities")
            .insertOne(community)

        FeedManager.createFeedForCommunity(community.id, owner)

        UserManager.getUser(owner).joinCommunity(community.id)

        return community
    }

    /** If another community has already taken [name]. */
    suspend fun nameTaken(name: String): Boolean {
        return MONGO
            .getDatabase("communities")
            .getCollection<Community>("communities")
            .findOne(Community::name eq name) == null
    }

    /** Get the member count of a community */
    suspend fun getMemberCount(community: Long): Int {
        return MONGO
            .getDatabase("users")
            .getCollection<Member>("members")
            .find(Member::member contains community)
            .toList()
            .size
    }

    /** Find members of [community] */
    suspend fun getMembers(community: Long): List<User> {
        return MONGO
            .getDatabase("users")
            .getCollection<Member>("members")
            .find(Member::member contains community)
            .toList()
            .map { member -> UserManager.getUser(member.id) }
    }

    /** Find users that have notifications on in [community] */
    suspend fun getNotifications(community: Long): List<User> {
        return MONGO
            .getDatabase("users")
            .getCollection<Member>("members")
            .find(Member::notifications contains community)
            .toList()
            .map { member -> UserManager.getUser(member.id) }
    }

    /** Change a [community]'s [name] */
    suspend fun changeName(community: Long, name: String) {
        MONGO
            .getDatabase("community")
            .getCollection<Community>("community")
            .updateOne(Community::id eq community, setValue(Community::name, name))
    }

    /** Change a [community]'s [description] */
    suspend fun changeDescription(community: Long, description: String) {
        MONGO
            .getDatabase("community")
            .getCollection<Community>("community")
            .updateOne(Community::id eq community, setValue(Community::description, description))
    }
}
