package net.unifey.handle.beta

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Refill
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import java.time.Duration
import net.unifey.auth.isAuthenticated
import net.unifey.handle.InvalidArguments
import net.unifey.response.Response
import net.unifey.util.PageRateLimit
import net.unifey.util.checkIpRateLimit

val rateLimit = PageRateLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1))))

fun Routing.betaPages() {
    route("/beta") {
        put("/request") {
            call.checkIpRateLimit(rateLimit)

            val token =
                try {
                    call.isAuthenticated()
                } catch (ex: Throwable) {
                    null
                }

            val args = call.receiveParameters()

            val name = token?.getOwner()?.username ?: args["name"]
            val message = args["message"]
            val type =
                try {
                    BetaHandler.RequestType.valueOf(args["type"] ?: "")
                } catch (e: IllegalArgumentException) {
                    null
                }

            if (name == null || message == null || type == null)
                throw InvalidArguments("name", "message", "type")

            BetaHandler.createRequest(type, message, name, token != null)

            call.respond(Response("OK"))
        }
    }
}
