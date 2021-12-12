package net.unifey.handle.users.profile.cosmetics

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import net.unifey.auth.isAuthenticated
import net.unifey.handle.InvalidArguments
import net.unifey.handle.NoPermission
import net.unifey.handle.NotFound
import net.unifey.handle.S3ImageHandler
import net.unifey.handle.users.GlobalRoles
import net.unifey.handle.users.UserManager
import net.unifey.response.Response
import net.unifey.util.cleanInput
import net.unifey.util.ensureProperImageBody

fun cosmeticPages(): Route.() -> Unit = {
    /**
     * Helper function for cosmetic management calls.
     * Returns the type to the ID.
     */
    suspend fun ApplicationCall.manageCosmetic(): Triple<Int, String, String?> {
        val token = isAuthenticated()

        if (UserManager.getUser(token.owner).role != GlobalRoles.ADMIN)
            throw NoPermission()

        val params = request.queryParameters

        val id = params["id"]
        val type = params["type"]?.toIntOrNull()

        if (id == null || type == null)
            throw InvalidArguments("type", "id")

        return Triple(type, cleanInput(id), params["desc"])
    }

    /**
     * Get an image cosmetic's image.
     */
    get("/viewer") {
        val params = call.request.queryParameters

        val type = params["type"]?.toIntOrNull()
        val id = params["id"]

        if (type == null || id == null)
            throw InvalidArguments("type", "id")

        call.respondBytes(S3ImageHandler.getPicture("cosmetics/$type.$id.jpg", "cosmetics/default.jpg"))
    }

    /**
     * Get all cosmetics, or select by type or id. To access a user's cosmetics, get their profile which contains their cosmetics.
     */
    get {
        val params = call.request.queryParameters

        val id = params["id"]
        val type = params["type"]?.toIntOrNull()

        val cosmetics = when {
            id != null && type != null -> Cosmetics.getAll().filter { cos -> cos.id.equals(id, true) && cos.type == type }
            id != null -> Cosmetics.getAll().filter { cos -> cos.id.equals(id, true) }
            type != null -> Cosmetics.getAll().filter { cos -> cos.type == type }

            else -> Cosmetics.getAll()
        }

        call.respond(cosmetics)
    }

    /**
     * Toggle a cosmetic for a user.
     */
    post {
        val token = call.isAuthenticated()

        if (UserManager.getUser(token.owner).role != GlobalRoles.ADMIN)
            throw NoPermission()

        val params = call.receiveParameters()

        val user = params["user"]?.toLongOrNull()
        val id = params["id"]
        val type = params["type"]?.toIntOrNull()

        if (id == null || type == null || user == null)
            throw InvalidArguments("type", "id", "user")

        val userObj = UserManager.getUser(user)

        val retrieved = Cosmetics.getAll()
            .firstOrNull { cosmetic -> cosmetic.type == type && cosmetic.id.equals(id, true) }
            ?: throw NotFound("cosmetic")

        val newCosmetics = userObj.profile.cosmetics.toMutableList()

        if (newCosmetics.any { cos -> cos.id.equals(id, true) && cos.type == type })
            newCosmetics.removeIf { cos -> cos.id.equals(id, true) && cos.type == type }
        else
            newCosmetics.add(retrieved)

        userObj.profile.cosmetics = newCosmetics

        call.respond(Response())
    }

    /**
     * Create a cosmetic
     */
    put {
        val (type, id, desc) = call.manageCosmetic()

        if (desc == null)
            throw InvalidArguments("desc")

        when (type) {
            0 -> {
                val badge = call.ensureProperImageBody()

                S3ImageHandler.upload("cosmetics/${type}.${id}.jpg", badge)
            }
        }

        Cosmetics.uploadCosmetic(type, id, desc)

        call.respond(Response())
    }

    /**
     * Delete a cosmetic
     */
    delete {
        val (type, id) = call.manageCosmetic()

        when (type) {
            0 -> {
                S3ImageHandler.delete("cosmetics/${type}.${id}.jpg")
            }
        }

        Cosmetics.deleteCosmetic(type, id)

        call.respond(Response())
    }
}