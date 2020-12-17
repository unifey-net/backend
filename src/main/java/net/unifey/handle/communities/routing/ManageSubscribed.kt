package net.unifey.handle.communities.routing

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import net.unifey.handle.AlreadyExists
import net.unifey.handle.NoPermission
import net.unifey.handle.NotFound
import net.unifey.handle.communities.CommunityRoles
import net.unifey.response.Response

val MANAGE_SUBSCRIBED: Route.() -> Unit = {
    /**
     * Manage your personal communities. (joined communities)
     */
    route("/manage") {
        /**
         * Join a community.
         */
        put {
            val (user, community) = call.managePersonalCommunities()

            if (!user.member.isMemberOf(community.id)) {
                user.member.join(community.id)
                call.respond(Response())
            } else
                throw AlreadyExists("community", community.id.toString())
        }

        /**
         * Leave a community.
         */
        delete {
            val (user, community) = call.managePersonalCommunities()

            if (community.getRole(user.id) == CommunityRoles.OWNER)
                throw NoPermission()

            if (user.member.isMemberOf(community.id)) {
                user.member.leave(community.id)
                call.respond(Response())
            } else
                throw NotFound("community")
        }
    }
}