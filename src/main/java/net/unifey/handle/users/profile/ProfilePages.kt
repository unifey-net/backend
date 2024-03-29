package net.unifey.handle.users.profile

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.unifey.auth.isAuthenticated
import net.unifey.handle.InvalidArguments
import net.unifey.response.Response
import net.unifey.util.cleanInput

fun profilePages(): Route.() -> Unit = {
    /** Get [paramName] for a profile action. */
    @Throws(InvalidArguments::class)
    suspend fun ApplicationCall.profileInput(
        paramName: String,
        maxLength: Int
    ): Pair<Long, String> {
        val token = isAuthenticated()

        val params = receiveParameters()
        val param = params[paramName] ?: throw InvalidArguments(paramName)
        val properParam = cleanInput(param)

        if (properParam.length > maxLength || properParam.isBlank())
            throw InvalidArguments(paramName)

        return token.owner to param
    }

    /** Change the description */
    put("/description") {
        val (user, desc) = call.profileInput("description", Profile.MAX_DESC_LEN)

        ProfileManager.setDescription(user, desc)

        call.respond(Response("OK"))
    }

    /** Change the location */
    put("/location") {
        val (user, loc) = call.profileInput("location", Profile.MAX_LOC_LEN)

        ProfileManager.setLocation(user, loc)

        call.respond(Response("OK"))
    }

    /** Change the discord */
    put("/discord") {
        val (user, disc) = call.profileInput("discord", Profile.MAX_DISC_LEN)

        ProfileManager.setDiscord(user, disc)

        call.respond(Response("OK"))
    }
}
