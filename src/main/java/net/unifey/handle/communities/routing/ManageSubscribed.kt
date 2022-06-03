package net.unifey.handle.communities.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.unifey.auth.isAuthenticated
import net.unifey.handle.AlreadyExists
import net.unifey.handle.Error
import net.unifey.handle.NoPermission
import net.unifey.handle.NotFound
import net.unifey.handle.communities.CommunityManager
import net.unifey.handle.communities.CommunityRoles
import net.unifey.handle.communities.getRole
import net.unifey.handle.users.member.MemberManager.getMember
import net.unifey.handle.users.member.MemberManager.hasNotificationsOn
import net.unifey.handle.users.member.MemberManager.isMemberOf
import net.unifey.handle.users.member.MemberManager.joinCommunity
import net.unifey.handle.users.member.MemberManager.joinNotifications
import net.unifey.handle.users.member.MemberManager.leaveCommunity
import net.unifey.handle.users.member.MemberManager.leaveNotifications
import net.unifey.response.Response

val MANAGE_SUBSCRIBED: Route.() -> Unit = {
    /** View subscribed communities */
    get {
        val token = call.isAuthenticated()

        call.respond(token.getOwner().getMember().member)
    }

    /** Join a community. */
    put {
        val (user, community) = call.managePersonalCommunities()

        if (!user.isMemberOf(community.id)) {
            user.joinCommunity(community.id)
            call.respond(Response("OK"))
        } else throw AlreadyExists("community", community.id.toString())
    }

    /** Leave a community. */
    delete {
        val (user, community) = call.managePersonalCommunities()

        if (community.getRole(user.id) == CommunityRoles.OWNER)
            throw Error({
                call.respond(
                    HttpStatusCode.Unauthorized,
                    Response("You're the owner of this community!")
                )
            })

        if (user.isMemberOf(community.id)) {
            user.leaveCommunity(community.id)

            CommunityManager.userLeave(community.id, user.id)

            call.respond(Response("OK"))
        } else throw NotFound("community")
    }

    /** Manage notifications for communities. */
    route("/notifications") {
        /** Get all notification subscribed communities */
        get {
            val token = call.isAuthenticated()

            call.respond(token.getOwner().getMember().notifications)
        }

        /** Subscribe to notifications for a community. */
        put {
            val (user, community) = call.managePersonalCommunities()

            if (!user.hasNotificationsOn(community.id)) {
                user.joinNotifications(community.id)

                call.respond(Response("OK"))
            } else throw NoPermission()
        }

        /** Unsubscribe to notifications for a community. */
        delete {
            val (user, community) = call.managePersonalCommunities()

            if (user.hasNotificationsOn(community.id)) {
                user.leaveNotifications(community.id)

                call.respond(Response("OK"))
            } else throw NoPermission()
        }
    }
}
