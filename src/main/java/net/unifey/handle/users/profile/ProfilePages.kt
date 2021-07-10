package net.unifey.handle.users.profile

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import net.unifey.auth.isAuthenticated
import net.unifey.handle.InvalidArguments
import net.unifey.handle.users.User
import net.unifey.handle.users.UserManager
import net.unifey.response.Response
import net.unifey.util.cleanInput

fun profilePages(): Route.() -> Unit = {
    /**
     * Get [paramName] for a profile action.
     */
    @Throws(InvalidArguments::class)
    suspend fun ApplicationCall.profileInput(paramName: String, maxLength: Int): Pair<User, String> {
        val token = isAuthenticated()

        val params = receiveParameters()

        val param = params[paramName] ?: throw InvalidArguments(paramName)

        val properParam = cleanInput(param)

        if (properParam.length > maxLength || properParam.isBlank())
            throw InvalidArguments(paramName)

        return UserManager.getUser(token.owner) to param
    }

    /**
     * Change the description
     */
    put("/description") {
        val (user, desc) = call.profileInput("description", Profile.MAX_DESC_LEN)

        user.profile.description = desc

        call.respond(Response())
    }

    /**
     * Change the location
     */
    put("/location") {
        val (user, loc) = call.profileInput("location", Profile.MAX_LOC_LEN)

        user.profile.location = loc

        call.respond(Response())
    }

    /**
     * Change the discord
     */
    put("/discord") {
        val (user, disc) = call.profileInput("discord", Profile.MAX_DISC_LEN)

        user.profile.discord = disc

        call.respond(Response())
    }
}