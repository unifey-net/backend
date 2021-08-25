package net.unifey.handle.users.friends

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import net.unifey.auth.isAuthenticated
import net.unifey.handle.InvalidArguments
import net.unifey.handle.live.Live
import net.unifey.handle.users.UserManager
import net.unifey.handle.users.responses.FriendResponse
import net.unifey.handle.users.responses.ReceivedFriendRequestResponse
import net.unifey.handle.users.responses.SentFriendRequestResponse
import net.unifey.response.Response
import org.json.JSONObject

suspend fun updateFriendOnline(userLive: Long) {
    val usersOnline = Live.getOnlineUsers()

    UserManager.getUser(userLive)
        .getFriends()
        .filter { friend -> usersOnline.contains(friend.id) }
        .forEach { friend ->
            val message = JSONObject()
                .put("friend", UserManager.getUser(friend.id).username)
                .put("id", friend.id)
                .put("online", FriendManager.getOnlineFriendCount(friend.id))

            Live.CHANNEL.send(Live.LiveObject("FRIEND_ONLINE", friend.id, message))
        }
}

suspend fun updateFriendOffline(userLive: Long) {
    val usersOnline = Live.getOnlineUsers()

    UserManager.getUser(userLive)
        .getFriends()
        .filter { friend -> usersOnline.contains(friend.id) }
        .forEach { friend ->
            val message = JSONObject()
                .put("friend", UserManager.getUser(friend.id).username)
                .put("id", friend.id)
                .put("online", FriendManager.getOnlineFriendCount(friend.id))

            Live.CHANNEL.send(Live.LiveObject("FRIEND_OFFLINE", friend.id, message))
        }
}

/**
 * Manage friends.
 */
fun friendsPages(): Route.() -> Unit = {
    put {
        val token = call.isAuthenticated()

        val params = call.receiveParameters()
        val id = params["id"]?.toLong()
            ?: throw InvalidArguments("id")

        FriendManager.sendFriendRequest(token.owner, id)

        call.respond(Response())
    }

    put("/name") {
        val token = call.isAuthenticated()

        val params = call.receiveParameters()
        val name = params["name"]
            ?: throw InvalidArguments("name")

        FriendManager.sendFriendRequest(token.owner, UserManager.getId(name))

        call.respond(Response())
    }

    delete("/{id}") {
        val token = call.isAuthenticated()

        val id = call.parameters["id"]?.toLongOrNull()
            ?: throw InvalidArguments("p_id")

        token.getOwner().removeFriend(id)
        call.respond(Response())
    }

    get {
        val token = call.isAuthenticated()

        call.respond(
            token
                .getOwner()
                .getFriends()
                .map { friend -> FriendResponse(friend.id, friend.friendedAt, UserManager.getUser(friend.id)) }
        )
    }

    route("/requests") {
        get {
            val token = call.isAuthenticated()

            call.respond(
                FriendManager
                    .getFriendRequests(token.owner)
                    .map { req -> ReceivedFriendRequestResponse(req, UserManager.getUser(req.sentTo)) }
            )
        }

        get("/sent") {
            val token = call.isAuthenticated()

            call.respond(
                FriendManager
                    .getPendingFriendRequests(token.owner)
                    .map { req -> SentFriendRequestResponse(req, UserManager.getUser(req.sentTo)) }
            )
        }

        /**
         * Delete a sent friend request
         */
        delete("/sent/{id}") {
            val token = call.isAuthenticated()

            val id = call.parameters["id"]?.toLongOrNull()
                ?: throw InvalidArguments("p_id")

            FriendManager
                .deleteFriendRequest(token.owner, id)

            call.respond(Response())
        }

        /**
         * Deny a friend request
         */
        delete("/{id}") {
            val token = call.isAuthenticated()

            val id = call.parameters["id"]?.toLongOrNull()
                ?: throw InvalidArguments("p_id")

            FriendManager.deleteFriendRequest(id, token.owner)

            call.respond(Response())
        }

        /**
         * Accept a friend request.
         */
        put {
            val token = call.isAuthenticated()

            val params = call.receiveParameters()
            val id = params["id"]?.toLong()
                ?: throw InvalidArguments("id")

            FriendManager.acceptFriendRequest(id, token.owner)

            call.respond(Response())
        }
    }
}