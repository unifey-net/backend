package net.unifey.handle.users.friends

import net.unifey.handle.AlreadyExists
import net.unifey.handle.InvalidArguments
import net.unifey.handle.InvalidVariableInput
import net.unifey.handle.NotFound
import net.unifey.handle.live.Live
import net.unifey.handle.mongo.MONGO
import net.unifey.handle.notification.NotificationManager
import net.unifey.handle.users.User
import net.unifey.handle.users.UserManager
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.setValue

object FriendManager {
    class UserFriends(val id: Long, val friends: List<Friend>)

    /** Get a user's friends,. */
    suspend fun User.getFriends() = getFriends(id)

    /** Remove a user's [friend]. */
    suspend fun User.removeFriend(friend: Long) = removeFriend(id, friend)

    /** If [User] has [friend]. */
    suspend fun User.hasFriend(friend: Long) = getFriends().any { user -> user.id == friend }

    /** Get [user]'s online friend count. */
    suspend fun getOnlineFriendCount(user: Long): Int {
        val online = Live.getOnlineUsers()

        return getFriends(user).map { friend -> online.contains(friend.id) }.size
    }

    /** Add [friend] to [id]'s friends. */
    @Throws(InvalidArguments::class)
    suspend fun addFriend(id: Long, friend: Long) {
        val hasFriends = hasFriends(id)

        if (!hasFriends) {
            MONGO
                .getDatabase("users")
                .getCollection<UserFriends>("friends")
                .insertOne(UserFriends(id, listOf(Friend(friend, System.currentTimeMillis()))))
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
    private suspend fun updateFriends(id: Long, friends: List<Friend>) {
        MONGO
            .getDatabase("users")
            .getCollection<UserFriends>("friends")
            .updateOne(UserFriends::id eq id, setValue(UserFriends::friends, friends))
    }

    /** Remove [friend] from [id]'s friends. */
    @Throws(NotFound::class)
    suspend fun removeFriend(id: Long, friend: Long) {
        suspend fun remove(id: Long, friend: Long) {
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
    suspend fun getFriends(id: Long): MutableList<Friend> {
        if (!hasFriends(id)) return arrayListOf()

        val doc =
            MONGO
                .getDatabase("users")
                .getCollection<UserFriends>("friends")
                .findOne(UserFriends::id eq id)

        if (doc != null) return doc.friends.toMutableList() else throw NotFound("friends")
    }

    /** If [id] has friends. */
    private suspend fun hasFriends(id: Long): Boolean =
        MONGO
            .getDatabase("users")
            .getCollection<UserFriends>()
            .countDocuments(UserFriends::id eq id) > 0

    /** Get friend requests for [user]. */
    suspend fun getFriendRequests(user: Long): List<FriendRequest> {
        return MONGO
            .getDatabase("users")
            .getCollection<FriendRequest>("friend_requests")
            .find(FriendRequest::sentTo eq user)
            .toList()
    }

    /** Get the friend requests [user] has sent, but the receiver has yet to accept/deny. */
    suspend fun getPendingFriendRequests(user: Long): List<FriendRequest> {
        return MONGO
            .getDatabase("users")
            .getCollection<FriendRequest>("friend_requests")
            .find(FriendRequest::sentFrom eq user)
            .toList()
    }

    /** Delete a friend request from [from]. (This is also the function for denying.) */
    suspend fun deleteFriendRequest(from: Long, to: Long) {
        if (!getPendingFriendRequests(from).any { req -> req.sentTo == to })
            throw NotFound("request")

        MONGO
            .getDatabase("users")
            .getCollection<FriendRequest>("friend_requests")
            .deleteOne(and(FriendRequest::sentFrom eq from, FriendRequest::sentTo eq to))
    }

    /** [to] has accepted [from]'s request. */
    suspend fun acceptFriendRequest(from: Long, to: Long) {
        deleteFriendRequest(from, to) // this checks if it exists

        // they're friends with each other
        addFriend(from, to)
        addFriend(to, from)

        NotificationManager.postNotification(
            from,
            "${UserManager.getUser(to).username} has accepted your friend request!"
        )
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

        MONGO
            .getDatabase("users")
            .getCollection<FriendRequest>("friend_requests")
            .insertOne(friendRequestObject)

        NotificationManager.postNotification(
            to,
            "${UserManager.getUser(from).username} has sent you a friend request!"
        )
    }
}
