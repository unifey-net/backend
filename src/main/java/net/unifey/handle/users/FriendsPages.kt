package net.unifey.handle.users

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.*
import net.unifey.auth.isAuthenticated
import net.unifey.handle.InvalidArguments
import net.unifey.response.Response

/**
 * Manage friends.
 */
fun Routing.friendsPages() {
    route("/friends") {
        put {
            val token = call.isAuthenticated()

            val params = call.receiveParameters()
            val id = params["id"]?.toLong()
                    ?: throw InvalidArguments("id")

            token.getOwner().addFriend(id)
            call.respond(Response())
        }

        delete {
            val token = call.isAuthenticated()

            val params = call.receiveParameters()
            val id = params["id"]?.toLong()
                    ?: throw InvalidArguments("id")

            token.getOwner().removeFriend(id)
            call.respond(Response())
        }

        get {
            val token = call.isAuthenticated()

            call.respond(token.getOwner().getFriends())
        }
    }
}