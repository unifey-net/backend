package net.unifey.handle.beta

import io.ktor.application.call
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.put
import io.ktor.routing.route
import net.unifey.handle.InvalidArguments
import net.unifey.response.Response

fun Routing.betaPages() {
    route("/beta") {
        put {
            val params = call.receiveParameters()

            val username = params["username"]
            val email = params["email"]

            if (username == null || email == null)
                throw InvalidArguments("username", "email")

            Beta.signUp(username, email)

            call.respond(Response())
        }
    }
}