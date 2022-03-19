package net.unifey.handle.emotes

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.routing.*
import net.unifey.auth.isAuthenticated
import net.unifey.handle.InvalidArguments
import net.unifey.handle.NoPermission
import net.unifey.handle.S3ImageHandler
import net.unifey.handle.communities.CommunityManager
import net.unifey.handle.communities.CommunityRoles
import net.unifey.handle.communities.getRole
import net.unifey.handle.users.UserManager
import net.unifey.response.Response
import net.unifey.util.ensureProperImageBody

fun Route.emotePages() {
    route("/emote") {
        get("/viewer/{parent}/{id}") {
            val parent = call.parameters["parent"]?.toLongOrNull()
            val id = call.parameters["id"]?.toLongOrNull()

            if (parent == null || id == null) throw InvalidArguments("parent", "id")

            call.respondBytes(ContentType.Image.JPEG, HttpStatusCode.OK) {
                S3ImageHandler.getPicture("emotes/${parent}.${id}.jpg", "")
            }
        }

        /** Get only global emotes */
        get { call.respond(EmoteHandler.getGlobalEmotes()) }

        /** Get community emotes and global emotes. */
        get("/{community}") {
            val community =
                call.parameters["community"]?.toLongOrNull() ?: throw InvalidArguments("community")

            val includeGlobal = call.request.queryParameters["global"]?.toBoolean() ?: true

            val obj = CommunityManager.getCommunityById(community)

            call.respond(EmoteHandler.getCommunityEmotes(obj, includeGlobal = includeGlobal))
        }

        /** Create an emote. */
        put {
            val token = call.isAuthenticated()
            val user = UserManager.getUser(token.owner)

            val params = call.request.queryParameters

            val parent = params["parent"]?.toLongOrNull()
            val name = params["name"]

            if (parent == null || name == null) throw InvalidArguments("parent", "name")

            val imageBytes = call.ensureProperImageBody(maxSize = 2_000_000)

            when {
                parent == -1L && user.role != 2 -> throw NoPermission()
                parent != -1L &&
                    CommunityManager.getCommunityById(parent).getRole(token.owner) <
                        CommunityRoles.ADMIN -> throw NoPermission()
            }

            EmoteHandler.createEmote(name, parent, token.owner, imageBytes)

            call.respond(Response("OK"))
        }
    }
}
