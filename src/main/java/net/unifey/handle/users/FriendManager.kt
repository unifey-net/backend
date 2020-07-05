package net.unifey.handle.users

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.mongodb.client.model.Filters
import net.unifey.handle.NotFound
import net.unifey.handle.mongo.Mongo
import org.bson.Document

object FriendManager {
    /**
     * Add [friend] to [id]'s friends.
     */
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

            friends.add(friend)

            updateFriends(id, friends)
        }
    }

    /**
     * Update [id]'s friends to [friends]
     */
    private fun updateFriends(id: Long, friends: MutableList<Long>) {
        Mongo.getClient()
                .getDatabase("users")
                .getCollection("friends")
                .updateOne(Filters.eq("id", id), Document(mapOf(
                        "friends" to friends
                )))
    }

    /**
     * Remove [friend] from [id]'s friends.
     */
    @Throws(NotFound::class)
    fun removeFriend(id: Long, friend: Long) {
        if (!hasFriends(id))
            return

        val friends = getFriends(id)
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
            return doc.getList("friends", Long::class.java).toMutableList()
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