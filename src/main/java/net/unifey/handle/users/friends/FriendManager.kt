package net.unifey.handle.users.friends

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import net.unifey.handle.AlreadyExists
import net.unifey.handle.InvalidArguments
import net.unifey.handle.InvalidVariableInput
import net.unifey.handle.NotFound
import net.unifey.handle.live.Live
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.notification.NotificationManager
import net.unifey.handle.users.UserManager
import org.bson.Document

object FriendManager {
    /** Get [user]'s online friend count. */
    fun getOnlineFriendCount(user: Long): Int {
        val online = Live.getOnlineUsers()

        return getFriends(user).map { friend -> online.contains(friend.id) }.size
    }

    /** Add [friend] to [id]'s friends. */
    @Throws(InvalidArguments::class)
    suspend fun addFriend(id: Long, friend: Long) {
        val hasFriends = hasFriends(id)

        if (!hasFriends) {
            Mongo.getClient()
                .getDatabase("users")
                .getCollection("friends")
                .insertOne(
                    Document(
                        mapOf(
                            "id" to id,
                            "friends" to
                                listOf(
                                    Document(
                                        mapOf(
                                            "id" to friend,
                                            "friendedAt" to System.currentTimeMillis()))))))
        } else {
            val friends = getFriends(id)

            if (friends.any { obj -> obj.id == friend }) throw InvalidArguments("friend")

            // ensure the friend is real :(
            UserManager.getUser(friend)

            friends.add(Friend(friend, System.currentTimeMillis()))

            updateFriends(id, friends)
        }
    }

    /** Update [id]'s [friends]. */
    private fun updateFriends(id: Long, friends: List<Friend>) {
        Mongo.getClient()
            .getDatabase("users")
            .getCollection("friends")
            .updateOne(
                Filters.eq("id", id),
                Updates.set(
                    "friends",
                    friends.map { friend ->
                        Document(mapOf("id" to friend.id, "friendedAt" to friend.friendedAt))
                    }))
    }

    /** Remove [friend] from [id]'s friends. */
    @Throws(NotFound::class)
    fun removeFriend(id: Long, friend: Long) {
        fun remove(id: Long, friend: Long) {
            if (!hasFriends(id)) return

            val friends = getFriends(id)

            if (!friends.any { obj -> obj.id == friend }) throw NotFound("friend")

            friends.removeIf { obj -> obj.id == friend }

            updateFriends(id, friends)
        }

        // they're friends with each other
        remove(id, friend)
        remove(friend, id)
    }

    /** Get [id]'s friends. */
    @Throws(NotFound::class)
    fun getFriends(id: Long): MutableList<Friend> {
        if (!hasFriends(id)) return arrayListOf()

        val doc =
            Mongo.getClient()
                .getDatabase("users")
                .getCollection("friends")
                .find(Filters.eq("id", id))
                .singleOrNull()

        if (doc != null)
            return doc.getList("friends", Document::class.java)
                .map { docu -> Friend(docu.getLong("id"), docu.getLong("friendedAt")) }
                .toMutableList()
        else throw NotFound("friends")
    }

    /** If [id] has friends. */
    private fun hasFriends(id: Long): Boolean =
        Mongo.getClient()
            .getDatabase("users")
            .getCollection("friends")
            .find(Filters.eq("id", id))
            .singleOrNull() != null

    /** Form a [FriendRequest] from a [Document] */
    private fun formFriendRequest(doc: Document) =
        FriendRequest(doc.getLong("sentAt"), doc.getLong("sentTo"), doc.getLong("sentFrom"))

    /** Get friend requests for [user]. */
    fun getFriendRequests(user: Long): List<FriendRequest> {
        return Mongo.getClient()
            .getDatabase("users")
            .getCollection("friend_requests")
            .find(Filters.eq("sentTo", user))
            .map(::formFriendRequest)
            .toList()
    }

    /** Get the friend requests [user] has sent, but the receiver has yet to accept/deny. */
    fun getPendingFriendRequests(user: Long): List<FriendRequest> {
        return Mongo.getClient()
            .getDatabase("users")
            .getCollection("friend_requests")
            .find(Filters.eq("sentFrom", user))
            .map(::formFriendRequest)
            .toList()
    }

    /** Delete a friend request from [from]. (This is also the function for denying.) */
    fun deleteFriendRequest(from: Long, to: Long) {
        if (!getPendingFriendRequests(from).any { req -> req.sentTo == to })
            throw NotFound("request")

        Mongo.getClient()
            .getDatabase("users")
            .getCollection("friend_requests")
            .deleteOne(Filters.and(Filters.eq("sentFrom", from), Filters.eq("sentTo", to)))
    }

    /** [to] has accepted [from]'s request. */
    suspend fun acceptFriendRequest(from: Long, to: Long) {
        deleteFriendRequest(from, to) // this checks if it exists

        // they're friends with each other
        addFriend(from, to)
        addFriend(to, from)

        NotificationManager.postNotification(
            from, "${UserManager.getUser(to).username} has accepted your friend request!")
    }

    /** Send a friend request. */
    suspend fun sendFriendRequest(from: Long, to: Long) {
        when {
            getPendingFriendRequests(from).any { req -> req.sentTo == to } ->
                throw AlreadyExists("to", "You've already sent this user a friend request.")
            from == to ->
                throw InvalidVariableInput("to", "You're sending a friend request to yourself.")
            getFriends(from).any { friend -> friend.id == to } ->
                throw AlreadyExists("friend", "You're already friends!")
        }

        // make sure the user exists
        UserManager.getUser(to)

        val friendRequestObject = FriendRequest(System.currentTimeMillis(), to, from)

        Mongo.getClient()
            .getDatabase("users")
            .getCollection("friend_requests")
            .insertOne(
                Document(
                    mapOf(
                        "sentAt" to friendRequestObject.sentAt,
                        "sentTo" to friendRequestObject.sentTo,
                        "sentFrom" to friendRequestObject.sentFrom)))

        NotificationManager.postNotification(
            to, "${UserManager.getUser(from).username} has sent you a friend request!")
    }
}
