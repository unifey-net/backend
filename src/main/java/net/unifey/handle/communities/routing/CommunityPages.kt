package net.unifey.handle.communities.routing

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import net.unifey.Unifey
import net.unifey.auth.isAuthenticated
import net.unifey.handle.Error
import net.unifey.handle.InvalidArguments
import net.unifey.handle.communities.CommunityInputRequirements
import net.unifey.handle.communities.CommunityManager
import net.unifey.handle.live.socketLogger
import net.unifey.response.Response
import net.unifey.util.checkCaptcha
import org.mindrot.jbcrypt.BCrypt

fun Routing.communityPages() {
    route("/community") {
        route("/manage", MANAGE_SUBSCRIBED)
        route("/{id}", MANAGE_COMMUNITY)

        /** Get a community by it's name. */
        get("/name/{name}") {
            val name = call.parameters["name"] ?: throw InvalidArguments("p_name")

            val community = CommunityManager.getCommunityByName(name)

            call.respondCommunity(community)
        }

        /** Create a community */
        put {
            val token = call.isAuthenticated()
            val params = call.receiveParameters()

            // only check for captcha in production
            if (Unifey.prod) call.checkCaptcha(params)

//            if (!CommunityManager.canCreate(token.owner))
//                throw Error({
//                    call.respond(
//                        HttpStatusCode.Unauthorized,
//                        Response(
//                            "Your account must be 14 days old and you can't have create a community before!"
//                        )
//                    )
//                })


            val name = params["name"]
            val desc = params["description"]
            val password = params["password"]

            if (!BCrypt.checkpw(password, token.getOwner().password))
                throw Error({ respond(HttpStatusCode.Unauthorized, Response("Invalid password!")) })

            if (name == null || desc == null) throw InvalidArguments("name", "description")

            CommunityInputRequirements.meets(
                listOf(
                    name to CommunityInputRequirements.NAME,
                    desc to CommunityInputRequirements.DESCRIPTION
                )
            )

            socketLogger.trace("{}, {}, {}", token.owner, name, desc)

            val createdCommunity = CommunityManager.createCommunity(token.owner, name, desc)

            call.respond(createdCommunity)
        }

        /** Get all communities */
        get {
            val params = call.request.queryParameters

            val page = params["page"]?.toIntOrNull() ?: 1

            call.respond(CommunityManager.getCommunities(page))
        }
    }
}
