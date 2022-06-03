package net.unifey.handle.users.profile.cosmetics

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.unifey.auth.isAuthenticated
import net.unifey.handle.InvalidArguments
import net.unifey.handle.NoPermission
import net.unifey.handle.NotFound
import net.unifey.handle.S3ImageHandler
import net.unifey.handle.users.GlobalRoles
import net.unifey.handle.users.UserManager
import net.unifey.handle.users.profile.ProfileManager
import net.unifey.handle.users.profile.cosmetics.Cosmetics.cosmetics
import net.unifey.response.Response

fun cosmeticPages(): Route.() -> Unit = {
    /** Get an image cosmetic's image. */
    get("/viewer") {
        val params = call.request.queryParameters

        val id = params["id"] ?: throw InvalidArguments("id")
        val cosmetic = Cosmetics.getById(id)

        if (cosmetic !is Cosmetics.Badge) throw NotFound("cosmetic")

        call.respondBytes(
            S3ImageHandler.getPicture("cosmetics/$id.jpg", "cosmetics/default.jpg")
        )
    }

    /**
     * Get all cosmetic.
     */
    get {
        call.respond(cosmetics)
    }

    /** Toggle a cosmetic for a user. */
    post {
        val token = call.isAuthenticated()

        if (UserManager.getUser(token.owner).role != GlobalRoles.ADMIN) throw NoPermission()

        val params = call.receiveParameters()

        val user = params["user"]?.toLongOrNull()
        val id = params["id"]

        if (id == null || user == null) throw InvalidArguments("id", "user")

        val userObj = UserManager.getUser(user)

        val cosmetic = Cosmetics.getById(id)
        val cosmetics = ProfileManager.getProfile(userObj.id).cosmetics

        if (cosmetics.any { cos -> cos == cosmetic.id }) {
            ProfileManager.removeCosmetic(userObj.id, cosmetic.id)
        } else {
            ProfileManager.addCosmetic(userObj.id, cosmetic.id)
        }

        call.respond(Response("OK"))
    }
}
