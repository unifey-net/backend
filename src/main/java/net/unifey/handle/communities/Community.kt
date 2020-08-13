package net.unifey.handle.communities

import com.fasterxml.jackson.annotation.JsonIgnore
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import dev.shog.lib.util.eitherOr
import net.unifey.handle.NotFound
import net.unifey.handle.communities.rules.CommunityRule
import net.unifey.handle.feeds.FeedManager
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.UserManager

class Community(
        val id: Long,
        val createdAt: Long,
        postRole: Int,
        viewRole: Int,
        commentRole: Int,
        name: String,
        description: String,
        val rules: MutableList<CommunityRule>,
        @JsonIgnore
        val roles: MutableMap<Long, Int>
) {
    /**
     * The size of the community.
     */
    val size
        get() = CommunityManager.getMemberCount(id)


    /**
     * The role where users are allowed to comment.
     */
    var commentRole = commentRole
        set(value) {
            Mongo.getClient()
                    .getDatabase("communities")
                    .getCollection("communities")
                    .updateOne(Filters.eq("id", id), Updates.set("permissions.comment_role", value))

            field = value
        }

    /**
     * The role where users are allowed to post.
     */
    var postRole = postRole
        set(value) {
            Mongo.getClient()
                    .getDatabase("communities")
                    .getCollection("communities")
                    .updateOne(Filters.eq("id", id), Updates.set("permissions.post_role", value))

            field = value
        }

    /**
     * The role where users are allowed to view the communities' feed.
     */
    var viewRole = viewRole
        set(value) {
            Mongo.getClient()
                    .getDatabase("communities")
                    .getCollection("communities")
                    .updateOne(Filters.eq("id", id), Updates.set("permissions.view_role", value))

            field = value
        }

    /**
     * The communities name.
     */
    var name = name
        set(value) {
            Mongo.getClient()
                    .getDatabase("communities")
                    .getCollection("communities")
                    .updateOne(Filters.eq("id", id), Updates.set("name", value))

            field = value
        }

    /**
     * The communities description.
     */
    var description = description
        set(value) {
            Mongo.getClient()
                    .getDatabase("communities")
                    .getCollection("communities")
                    .updateOne(Filters.eq("id", id), Updates.set("description", value))

            field = value
        }

    /**
     * Update [user]'s role.
     */
    fun setRole(user: Long, role: Int) {
        roles[user] = role

        when {
            role >= CommunityRoles.MODERATOR -> {
                val feed = getFeed()

                if (!feed.moderators.contains(user)) {
                    feed.moderators.add(user)
                    feed.update()
                }
            }

            CommunityRoles.MODERATOR > role -> {
                val feed = getFeed()

                if (feed.moderators.contains(user)) {
                    feed.moderators.remove(user)
                    feed.update()
                }
            }
        }


        Mongo.getClient()
                .getDatabase("communities")
                .getCollection("communities")
                .updateOne(Filters.eq("id", id), Updates.set("roles.$user", role))
    }

    /**
     * Remove [user]'s role.
     */
    fun removeRole(user: Long) {
        roles.remove(user)

        val feed = getFeed()

        if (feed.moderators.contains(user)) {
            feed.moderators.remove(user)
            feed.update()
        }

        Mongo.getClient()
                .getDatabase("communities")
                .getCollection("communities")
                .updateOne(Filters.eq("id", id), Updates.unset("roles.$user"))
    }

    /**
     * Get [user]'s role.
     */
    fun getRole(user: Long): Int? {
        if (user == -1L)
            return CommunityRoles.DEFAULT

        val role = roles[user]

        return role
                ?: UserManager.getUser(user) // if they're in the community, give them member
                        .member
                        .isMemberOf(id)
                        .eitherOr(CommunityRoles.MEMBER, CommunityRoles.DEFAULT)
    }

    /**
     * Get the communities feed.
     */
    fun getFeed() =
            FeedManager.getCommunityFeed(this) ?:
                    throw NotFound("community feed")
}