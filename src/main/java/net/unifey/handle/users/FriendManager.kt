package net.unifey.handle.users

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.unifey.DatabaseHandler

object FriendManager {
    fun addFriend(uid: Long, friend: Long) {
        val hasFriends = hasFriends(uid)
        println("FriendManager#addFriend hasFriends = $hasFriends")
        if (!hasFriends) {
            var stmt = DatabaseHandler.getConnection().prepareStatement("INSERT INTO friends (uid, friends) VALUES (?, ?)")
            stmt.setLong(1, uid)
            var friendsList = ArrayList<Long>()
            friendsList.add(friend)
            stmt.setString(2, ObjectMapper().writeValueAsString(friendsList))
            stmt.executeQuery()
        } else {
            val friends = getFriends(uid) ?: return
            friends.add(friend)
            updateFriends(uid, friends)
        }
    }

    private fun updateFriends(uid: Long, friends: ArrayList<Long>) {
        var stmt = DatabaseHandler.getConnection().prepareStatement("UPDATE friends SET friends = ? WHERE uid = ?")
        stmt.setString(1, ObjectMapper().writeValueAsString(friends))
        stmt.setLong(2, uid)
        stmt.executeQuery()
    }

    fun removeFriend(uid: Long, friend: Long) {
        if (!hasFriends(uid))
            return
        var friends = getFriends(uid) ?: return
        friends.remove(friend)
        updateFriends(uid, friends)
    }

    fun getFriends(uid: Long): ArrayList<Long>? {
        if (!hasFriends(uid))
            return null
        val stmt = DatabaseHandler.getConnection()
                .prepareStatement("SELECT * FROM friends WHERE uid = ?")

        stmt.setLong(1, uid)
        val rs = stmt.executeQuery()

        return if (rs.next())
            ObjectMapper().readValue<ArrayList<Long>>(rs.getString("friends"))
        else throw Exception("No next RS")
    }

    private fun hasFriends(uid: Long): Boolean {
        val stmt = DatabaseHandler.getConnection()
                .prepareStatement("SELECT * FROM friends WHERE uid = ?")

        stmt.setLong(1, uid)
        val rs = stmt.executeQuery()

        return rs.next()
    }
}