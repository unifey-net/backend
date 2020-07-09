package net.unifey.handle.communities

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.routing.*
import net.unifey.auth.ex.AuthenticationException
import net.unifey.auth.isAuthenticated
import net.unifey.handle.*
import net.unifey.handle.communities.responses.GetCommunityResponse
import net.unifey.handle.S3ImageHandler
import net.unifey.handle.emotes.EmoteHandler
import net.unifey.response.Response

suspend fun ApplicationCall.respondCommunity(community: Community) {
    val user = try {
        isAuthenticated()
    } catch (authEx: AuthenticationException) {
        null
    }

    if (community.viewRole != CommunityRoles.DEFAULT) {
        if (user == null || community.getRole(user.owner) != community.viewRole) {
            respond(GetCommunityResponse(
                    community,
                    community.getRole(user?.owner ?: -1),
                    null,
                    null
            ))

            return
        }
    }

    respond(GetCommunityResponse(
            community,
            community.getRole(user?.owner ?: -1L),
            EmoteHandler.getCommunityEmotes(community),
            community.getFeed()
    ))
}

fun Routing.communityPages() {
    route("/community") {
        /**
         * Get a community by it's name.
         */
        get("/name/{name}") {
            val name = call.parameters["name"]
                    ?: throw InvalidArguments("p_name")

            val community = CommunityManager.getCommunityByName(name)

            call.respondCommunity(community)
        }

        route("/{id}") {
            get {
                val id = call.parameters["id"]?.toLongOrNull()
                        ?: throw InvalidArguments("p_id")

                val community = CommunityManager.getCommunityById(id)

                call.respondCommunity(community)
            }

            /**
             * Delete a community.
             */
            delete {
                val token = call.isAuthenticated()
                val id = call.parameters["id"]?.toLongOrNull()
                        ?: throw InvalidArguments("p_id")

                val community = CommunityManager.getCommunityById(id)

                if (community.getRole(token.owner) != CommunityRoles.OWNER)
                    throw NoPermission()

                CommunityManager.deleteCommunity(id)

                call.respond(Response())
            }

            /**
             * Get a communities' profile picture. TODO
             */
            get("/picture") {
            }

            /**
             * Change the name.
             *
             * TODO
             */
            put("/name") {
            }

            /**
             * Change the description.
             *
             * TODO
             */
            put("/description") {
            }
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

            when {
                name == null || desc == null ->
                    throw InvalidArguments("name", "desc")

                desc.length > 255 ->
                    throw ArgumentTooLarge("desc", 255)
            }

            try {
                CommunityManager.getCommunityByName(name!!)
            } catch (ex: NotFound) {
                throw AlreadyExists("community", "name")
            }

            call.respond(CommunityManager.createCommunity(token.owner, name, desc!!))
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