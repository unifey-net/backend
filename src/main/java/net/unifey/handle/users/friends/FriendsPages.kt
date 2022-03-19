package net.unifey.handle.users.friends

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import net.unifey.auth.isAuthenticated
import net.unifey.handle.InvalidArguments
import net.unifey.handle.live.*
import net.unifey.handle.live.SocketActionHandler.action
import net.unifey.handle.live.objs.ActionHolder
import net.unifey.handle.live.objs.FindActions
import net.unifey.handle.live.objs.SocketType
import net.unifey.handle.users.UserManager
import net.unifey.handle.users.friends.FriendManager.getFriends
import net.unifey.handle.users.friends.FriendManager.removeFriend
import net.unifey.handle.users.responses.FriendResponse
import net.unifey.handle.users.responses.ReceivedFriendRequestResponse
import net.unifey.handle.users.responses.SentFriendRequestResponse
import net.unifey.response.Response
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val friendOnlineStatus: Logger = LoggerFactory.getLogger("FRIEND STATUS")

/** When [userLive] goes online, notify their friends that they've gone online. */
suspend fun updateFriendOnline(userLive: Long) {
    friendOnlineStatus.trace("ONLINE $userLive")

    val usersOnline = Live.getOnlineUsers()

    UserManager.getUser(userLive)
        .getFriends()
        .filter { friend -> usersOnline.keys.contains(friend.id) }
        .forEach { friend ->
            val message =
                JSONObject()
                    .put("friend", UserManager.getUser(userLive).username)
                    .put("id", userLive)
                    .put("online", FriendManager.getOnlineFriendCount(friend.id))

            Live.sendUpdate(Live.LiveObject("FRIEND_ONLINE", friend.id, message))
        }
}

/** When [userLive] goes offline, notify their friends that they've gone offline. */
suspend fun updateFriendOffline(userLive: Long) {
    friendOnlineStatus.trace("OFFLINE $userLive")

    val usersOnline = Live.getOnlineUsers()

    UserManager.getUser(userLive)
        .getFriends()
        .filter { friend -> usersOnline.keys.contains(friend.id) }
        .forEach { friend ->
            val message =
                JSONObject()
                    .put("friend", UserManager.getUser(userLive).username)
                    .put("id", userLive)
                    .put("online", FriendManager.getOnlineFriendCount(friend.id))

            Live.sendUpdate(Live.LiveObject("FRIEND_OFFLINE", friend.id, message))
        }
}

/** Manage friends. */
fun friendsPages(): Route.() -> Unit = {
    put {
        val token = call.isAuthenticated()

        val params = call.receiveParameters()
        val id = params["id"]?.toLong() ?: throw InvalidArguments("id")

        FriendManager.sendFriendRequest(token.owner, id)

        call.respond(Response("OK"))
    }

    put("/name") {
        val token = call.isAuthenticated()

        val params = call.receiveParameters()
        val name = params["name"] ?: throw InvalidArguments("name")

        FriendManager.sendFriendRequest(token.owner, UserManager.getId(name))

        call.respond(Response("OK"))
    }

    delete("/{id}") {
        val token = call.isAuthenticated()

        val id = call.parameters["id"]?.toLongOrNull() ?: throw InvalidArguments("p_id")

        token.getOwner().removeFriend(id)
        call.respond(Response("OK"))
    }

    get {
        val token = call.isAuthenticated()

        call.respond(
            token.getOwner().getFriends().map { friend ->
                FriendResponse(friend.id, friend.friendedAt, UserManager.getUser(friend.id))
            }
        )
    }

    get("/search") {
        val token = call.isAuthenticated()

        val name = call.request.queryParameters["name"] ?: throw InvalidArguments("name")

        call.respond(
            Response(
                token
                    .getOwner()
                    .getFriends()
                    .map { friend -> UserManager.getUser(friend.id) }
                    .filter { friend -> friend.username.contains(name) }
            )
        )
    }

    route("/requests") {
        get {
            val token = call.isAuthenticated()

            call.respond(
                FriendManager.getFriendRequests(token.owner).map { req ->
                    ReceivedFriendRequestResponse(req, UserManager.getUser(req.sentFrom))
                }
            )
        }

        get("/sent") {
            val token = call.isAuthenticated()

            call.respond(
                FriendManager.getPendingFriendRequests(token.owner).map { req ->
                    SentFriendRequestResponse(req, UserManager.getUser(req.sentTo))
                }
            )
        }

        /** Delete a sent friend request */
        delete("/sent/{id}") {
            val token = call.isAuthenticated()

            val id = call.parameters["id"]?.toLongOrNull() ?: throw InvalidArguments("p_id")

            FriendManager.deleteFriendRequest(token.owner, id)

            call.respond(Response("OK"))
        }

        /** Deny a friend request */
        delete("/{id}") {
            val token = call.isAuthenticated()

            val id = call.parameters["id"]?.toLongOrNull() ?: throw InvalidArguments("p_id")

            FriendManager.deleteFriendRequest(id, token.owner)

            call.respond(Response("OK"))
        }

        /** Accept a friend request. */
        put {
            val token = call.isAuthenticated()

            val params = call.receiveParameters()
            val id = params["id"]?.toLong() ?: throw InvalidArguments("id")

            FriendManager.acceptFriendRequest(id, token.owner)

            call.respond(Response("OK"))
        }
    }
}

enum class FriendsActionsTypes : SocketType {
    GET_FRIENDS
}

/** Actions for friends. */
@FindActions
object FriendsActions : ActionHolder {
    override val pages: ArrayList<Pair<SocketType, SocketAction>> =
        SocketActionHandler.socketActions {
            action(FriendsActionsTypes.GET_FRIENDS) {
                val friends =
                    getFriends(token.owner).map { friend ->
                        FriendResponse(friend.id, friend.friendedAt, UserManager.getUser(friend.id))
                    }

                println(getFriends(token.owner))

                respondSuccess(friends)
            }
        }
}
