package net.unifey.auth.users

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import net.unifey.auth.isAuthenticated
import net.unifey.response.Response

fun Routing.userPages() {
    get("/user") {
        val token = call.isAuthenticated()

        call.respond(Response(UserManager.getUser(token.owner)))
    }

    get("/user/{name}") {
        val name = call.parameters["name"]

        if (name == null)
            call.respond(Response("No name parameter"))
        else
            call.respond(Response(UserManager.getUser(name)))
    }
}