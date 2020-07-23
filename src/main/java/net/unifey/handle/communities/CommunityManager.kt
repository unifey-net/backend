package net.unifey.handle.communities

import com.mongodb.client.model.Filters.eq
import dev.shog.lib.util.getAge
import net.unifey.handle.InvalidArguments
import net.unifey.handle.NotFound
import net.unifey.handle.feeds.FeedManager
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.UserManager
import net.unifey.util.IdGenerator
import org.bson.Document
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

/**
 * Manage [Community]s
 */
object CommunityManager {
    private val cache: MutableList<Community> = mutableListOf()

    /**
     * Delete a community by it's [id]
     */
    fun deleteCommunity(id: Long) {
        cache.removeIf { community -> community.id == id }

        Mongo.getClient()
                .getDatabase("communities")
                .getCollection("communities")
                .deleteOne(eq("id", id))
    }

    /**
     * Get a [Community] by [name].
     */
    fun getCommunityByName(name: String): Community {
        val cacheCommunity = cache.singleOrNull { community -> community.name == name }

        if (cacheCommunity != null)
            return cacheCommunity

        val obj = Mongo.getClient()
                .getDatabase("communities")
                .getCollection("communities")
                .find()
                .firstOrNull { doc -> doc.getString("name").equals(name, true) }
                ?: throw NotFound("community")

        return getCommunity(obj)
    }

    /**
     * Get a [Community] by [id].
     */
    fun getCommunityById(id: Long): Community {
        val cacheCommunity = cache.singleOrNull { community -> community.id == id }

        if (cacheCommunity != null)
            return cacheCommunity

        val obj = Mongo.getClient()
                .getDatabase("communities")
                .getCollection("communities")
                .find(eq("id", id))
                .firstOrNull()
                ?: throw NotFound("community")

        return getCommunity(obj)
    }

    /**
     * Parse a [Community] from a bson [doc].
     */
    private fun getCommunity(doc: Document): Community {
        val permissions = doc.get("permissions", Document::class.java)

        val community = Community(
                doc.getLong("id"),
                doc.getLong("created_at"),
                permissions.getInteger("post_role"),
                permissions.getInteger("view_role"),
                permissions.getInteger("comment_role"),
                doc.getString("name"),
                doc.getString("description"),
                doc.get("roles", Document::class.java)
                        .mapKeys { it.key.toLong() }
                        .mapValues { it.value as Int }
                        .toMutableMap()
        )

        cache.add(community)

        return community
    }

    /**
     * Get a page of communities.
     */
    fun getCommunities(page: Int): List<Community> {
        val size = getCommunitiesSize()

        // there's 15 communities to a page.
        val pageCount = ceil(size.toDouble() / 15.0).toInt()

        if (page > pageCount || 0 >= page)
            throw InvalidArguments("page")

        // the amount of documents to skip to get to the proper page.
        val skip = if (page != 1)
            (page - 1) * 15
        else
            0

        return Mongo.getClient()
                .getDatabase("communities")
                .getCollection("communities")
                .find()
                .skip(skip)
                .limit(15)
                .map { doc -> getCommunity(doc) }
                .toList()
    }

    /**
     * Get the amount of communities
     */
    private fun getCommunitiesSize(): Long =
            Mongo.getClient()
                    .getDatabase("communities")
                    .getCollection("communities")
                    .countDocuments()

    /**
     * If [id] can create a community
     *
     * They must be verified and have their account for over 14 days.
     */
    fun canCreate(id: Long): Boolean {
        val user = UserManager.getUser(id)

        return user.verified
                && user.createdAt.getAge() >= TimeUnit.DAYS.toMillis(14)
                && !hasCreatedCommunityBefore(id)
    }

    /**
     * if [id] has made a community before
     */
    private fun hasCreatedCommunityBefore(id: Long): Boolean {
        return Mongo.getClient()
                .getDatabase("communities")
                .getCollection("communities")
                .find(eq("roles.${id}", 4))
                .singleOrNull() != null
    }

    /**
     * Create a community.
     */
    fun createCommunity(owner: Long, name: String, desc: String): Community {
        val roles = hashMapOf(owner to CommunityRoles.OWNER)

        val community = Community(
                IdGenerator.getId(),
                System.currentTimeMillis(),
                CommunityRoles.MEMBER,
                CommunityRoles.DEFAULT,
                CommunityRoles.MEMBER,
                name,
                desc,
                roles
        )

        val communityDoc = Document(mapOf(
                "id" to community.id,
                "name" to community.name,
                "description" to community.description,
                "created_at" to community.createdAt,
                "permissions" to Document(mapOf(
                        "post_role" to community.postRole,
                        "view_role" to community.viewRole,
                        "comment_role" to community.commentRole
                )),
                "roles" to Document(mapOf(
                        "$owner" to CommunityRoles.OWNER
                ))
        ))

        Mongo.getClient()
                .getDatabase("communities")
                .getCollection("communities")
                .insertOne(communityDoc)

        FeedManager.createFeedForCommunity(community.id, owner)

        cache.add(community)

        return community
    }

    /**
     * If another community has already taken [name].
     */
    fun nameTaken(name: String): Boolean {
        return Mongo.getClient()
                .getDatabase("communities")
                .getCollection("communities")
                .find(eq("name", name))
                .any()
    }
}