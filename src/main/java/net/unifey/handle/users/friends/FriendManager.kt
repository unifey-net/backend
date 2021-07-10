package net.unifey.handle.users.friends

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import net.unifey.handle.InvalidArguments
import net.unifey.handle.NotFound
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.UserManager
import org.bson.Document

object FriendManager {
    /**
     * Add [friend] to [id]'s friends.
     */
    @Throws(InvalidArguments::class)
    fun addFriend(id: Long, friend: Long) {
        val hasFriends = hasFriends(id)

        if (!hasFriends) {
            Mongo.getClient()
                    .getDatabase("users")
                    .getCollection("friends")
                    .insertOne(Document(mapOf(
                            "id" to id,
                            "friends" to listOf(friend)
                    )))
        } else {
            val friends = getFriends(id)

            if (friends.contains(friend))
                throw InvalidArguments("friend")

            // ensure the friend is real :(
            UserManager.getUser(friend)

            friends.add(friend)

            updateFriends(id, friends)
        }
    }

    /**
     * Update [id]'s [friends].
     */
    private fun updateFriends(id: Long, friends: List<Long>) {
        Mongo.getClient()
                .getDatabase("users")
                .getCollection("friends")
                .updateOne(Filters.eq("id", id), Updates.set("friends", friends))
    }

    /**
     * Remove [friend] from [id]'s friends.
     */
    @Throws(NotFound::class)
    fun removeFriend(id: Long, friend: Long) {
        if (!hasFriends(id))
            return

        val friends = getFriends(id)

        if (!friends.contains(friend))
            throw NotFound("friend")

        friends.remove(friend)

        updateFriends(id, friends)
    }

    /**
     * Get [id]'s friends.
     */
    @Throws(NotFound::class)
    fun getFriends(id: Long): MutableList<Long> {
        if (!hasFriends(id))
            return arrayListOf()

        val doc = Mongo.getClient()
                .getDatabase("users")
                .getCollection("friends")
                .find(Filters.eq("id", id))
                .singleOrNull()

        if (doc != null)
            return doc["friends"] as MutableList<Long>
        else
            throw NotFound("friends")
    }

    /**
     * If [id] has friends.
     */
    private fun hasFriends(id: Long): Boolean =
            Mongo.getClient()
                    .getDatabase("users")
                    .getCollection("friends")
                    .find(Filters.eq("id", id))
                    .singleOrNull() != null
}