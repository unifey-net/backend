package net.unifey.handle.users

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.*
import net.unifey.auth.isAuthenticated
import net.unifey.response.Response

/**
 * TODO make sure user exists before adding
 * TODO make sure there's no duplicates
 */
fun Routing.friendsPages() {
    route("/friends") {
        put {
            val token = call.isAuthenticated()
            val userUID = token.owner

            val params = call.receiveParameters()
            val friendId = params["id"]?.toLong()

            if (friendId != null) {
                FriendManager.addFriend(userUID, friendId)
                call.respond(Response())
            } else
                call.respond(HttpStatusCode.BadRequest, Response("No id parameter"))
        }

        delete {
            val token = call.isAuthenticated()
            val userId = token.owner

            val params = call.receiveParameters()
            val friendId = params["id"]?.toLong()

            if (friendId != null) {
                FriendManager.removeFriend(userId, friendId)
                call.respond(Response())
            } else
                call.respond(HttpStatusCode.BadRequest, Response("No id parameter"))
        }

        get {
            val token = call.isAuthenticated()

            call.respond(FriendManager.getFriends(token.owner))
        }
    }
}