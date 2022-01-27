package net.unifey.handle.communities

import com.mongodb.client.model.Updates
import dev.ajkneisl.lib.util.eitherOr
import net.unifey.Unifey
import net.unifey.handle.feeds.FeedManager
import net.unifey.handle.mongo.MONGO
import net.unifey.handle.users.UserManager
import net.unifey.handle.users.member.MemberManager.isMemberOf
import org.litote.kmongo.eq
import org.litote.kmongo.setValue

/** Update [user]'s role. */
suspend fun Community.setRole(user: Long, role: Int) {
    roles[user] = role

    when {
        role >= CommunityRoles.MODERATOR -> {
            val feed = getFeed()

            if (!feed.moderators.contains(user)) {
                FeedManager.addModerator(feed.id, user)
            }
        }
        CommunityRoles.MODERATOR > role -> {
            val feed = getFeed()

            if (feed.moderators.contains(user)) {
                FeedManager.removeModerator(feed.id, user)
            }
        }
    }

    MONGO
        .getDatabase("communities")
        .getCollection<Community>()
        .updateOne(Community::id eq id, Updates.set("roles.$user", role))
}

/** Remove [user]'s role. */
suspend fun Community.removeRole(user: Long) {
    roles.remove(user)

    val feed = getFeed()

    if (feed.moderators.contains(user)) {
        FeedManager.removeModerator(feed.id, user)
    }

    MONGO
        .getDatabase("communities")
        .getCollection<Community>()
        .updateOne(Community::id eq id, Updates.unset("roles.$user"))
}

/** Get [user]'s role. */
suspend fun Community.getRole(user: Long): Int {
    if (user == -1L) return CommunityRoles.DEFAULT

    val role = roles[user]

    return role
        ?: UserManager.getUser(user) // if they're in the community, give them member
            .isMemberOf(id)
            .eitherOr(CommunityRoles.MEMBER, CommunityRoles.DEFAULT)
}

suspend fun Community.setPermissions(
    viewRole: Int = permissions.viewRole,
    postRole: Int = permissions.postRole,
    commentRole: Int = permissions.commentRole
) {
    val newPermissions = CommunityPermissions(viewRole, postRole, commentRole)

    // TODO different logger
    Unifey.ROOT_LOGGER.trace("Permission Update: ($id) $permissions -> $newPermissions")

    MONGO
        .getDatabase("community")
        .getCollection<Community>("community")
        .updateOne(Community::id eq id, setValue(Community::permissions, newPermissions))
}
