package net.unifey.handle.communities.routing

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import net.unifey.auth.isAuthenticated
import net.unifey.handle.InvalidArguments
import net.unifey.handle.NoPermission
import net.unifey.handle.communities.CommunityInputRequirements
import net.unifey.handle.communities.CommunityManager

fun Routing.communityPages() {
    route("/community") {
        route("/manage", MANAGE_SUBSCRIBED)
        route("/{id}", MANAGE_COMMUNITY)

        /**
         * Get a community by it's name.
         */
        get("/name/{name}") {
            val name = call.parameters["name"]
                    ?: throw InvalidArguments("p_name")

            val community = CommunityManager.getCommunityByName(name)

            call.respondCommunity(community)
        }

        /**
         * Create a community
         */
        put {
            val token = call.isAuthenticated()

            if (!CommunityManager.canCreate(token.owner))
                throw NoPermission()

            val params = call.receiveParameters()

            val name = params["name"]
            val desc = params["desc"]

            if (name == null || desc == null)
                throw InvalidArguments("name", "desc")

            CommunityInputRequirements.meets(listOf(
                    name to CommunityInputRequirements.NAME,
                    desc to CommunityInputRequirements.DESCRIPTION
            ))

            call.respond(CommunityManager.createCommunity(token.owner, name, desc))
        }

        /**
         * Get all communities
         */
        get {
            val params = call.request.queryParameters

            val page = params["page"]?.toIntOrNull() ?: 1

            call.respond(CommunityManager.getCommunities(page))
        }
    }
}