package net.unifey.auth.users

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import net.unifey.auth.isAuthenticated
import net.unifey.response.Response

fun Routing.userPages() {
    get("/user") {
        val token = call.isAuthenticated()

        call.respond(Response(UserManager.getUser(token.owner)))
    }

    get("/user/name/{name}") {
        val name = call.parameters["name"]

        if (name == null)
            call.respond(HttpStatusCode.BadRequest, Response("No name parameter"))
        else
            call.respond(Response(UserManager.getUser(name)))
    }

    get("/user/id/{id}") {
        val id = call.parameters["id"]?.toLongOrNull()

        if (id == null)
            call.respond(HttpStatusCode.BadRequest, Response("No id parameter"))
        else
            call.respond(Response(UserManager.getUser(id)))
    }

    post("/user/name") {
        val token = call.isAuthenticated()

        val params = call.receiveParameters()
        val username = params["username"]

        if (username == null)
            call.respond(HttpStatusCode.BadRequest, Response("No username parameter"))
        else {
            when {
                username.length > 16 ->
                    call.respond(HttpStatusCode.BadRequest, Response("Username is too long! (must be <=16)"))

                3 > username.length ->
                    call.respond(HttpStatusCode.BadRequest, Response("Username is too short! (must be >3)"))

                else -> {
                    UserManager.updateName(token.owner, username)

                    call.respond(HttpStatusCode.OK, Response("Changed username."))
                }
            }
        }
    }

    post("/user/email") {
        val token = call.isAuthenticated()

        val params = call.receiveParameters()
        val email = params["email"]

        if (email == null)
            call.respond(HttpStatusCode.BadRequest, Response("No email parameter"))
        else {
            when {
                email.length > 120 ->
                    call.respond(HttpStatusCode.BadRequest, Response("Email is too long! (must be <=120)"))

                !UserManager.EMAIL_REGEX.matches(email) ->
                    call.respond(HttpStatusCode.BadRequest, Response("Not a proper email!"))

                else -> {
                    UserManager.updateEmail(token.owner, email)

                    call.respond(HttpStatusCode.OK, Response("Changed email."))
                }
            }
        }
    }
}