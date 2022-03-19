package net.unifey.auth.connections

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Refill
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import java.time.Duration
import net.unifey.auth.connections.handlers.Google
import net.unifey.auth.connections.twofactor.TwoFactor
import net.unifey.auth.isAuthenticated
import net.unifey.handle.AlreadyExists
import net.unifey.handle.InvalidArguments
import net.unifey.response.Response
import net.unifey.util.PageRateLimit

/**
 * 1 token every 1 minute.
 *
 * This is due to the more expensive requests to the connection services. EX: grabbing a profile
 * every new google auth token.
 */
private val rateLimit = PageRateLimit(Bandwidth.classic(1, Refill.greedy(1, Duration.ofMinutes(1))))

fun connectionPages(): Route.() -> Unit = {
    put("/google") {
        val token = call.isAuthenticated(pageRateLimit = rateLimit)
        val params = call.receiveParameters()

        val authToken = params["authToken"] ?: throw InvalidArguments("authToken")

        if (ConnectionManager.findConnection(ConnectionManager.Type.GOOGLE, token.owner) != null)
            throw AlreadyExists("connection", "Google")

        val googleUser = Google.getServiceId(authToken) ?: throw InvalidArguments("authToken")

        ConnectionManager.createConnection(ConnectionManager.Type.GOOGLE, token.owner, googleUser)

        call.respond(Response("OK"))
    }

    route("/2fa") {
        delete {
            val user = call.isAuthenticated()

            TwoFactor.disableTwoFactor(user.owner)

            call.respond(Response("OK"))
        }

        put {
            @kotlinx.serialization.Serializable data class TwoFactorResponse(val key: String)

            val user = call.isAuthenticated()

            val key = TwoFactor.enableTwoFactor(user.owner)

            call.respond(TwoFactorResponse(key))
        }

        get {
            @kotlinx.serialization.Serializable data class TwoFactorResponse(val enabled: Boolean)

            val user = call.isAuthenticated()

            call.respond(TwoFactorResponse(TwoFactor.alreadyExists(user.owner)))
        }
    }
}
