package net.unifey.handle.users

import com.fasterxml.jackson.databind.ObjectMapper
import net.unifey.DatabaseHandler
import net.unifey.handle.ArgumentTooLarge
import net.unifey.handle.InvalidArguments
import net.unifey.handle.users.member.Member
import net.unifey.handle.users.profile.Profile

class User(
        val id: Long,
        username: String,
        password: String,
        email: String,
        role: Int,
        val createdAt: Long
) {
    /**
     * A user's profile. This contains profile details such as Discord.
     */
    val profile by lazy {
        val rs = DatabaseHandler.getConnection()
                .prepareStatement("SELECT * FROM profiles WHERE id = ?")
                .apply { setLong(1, id) }
                .executeQuery()

        if (rs.next()) {
            Profile(
                    id,
                    rs.getString("description"),
                    rs.getString("discord"),
                    rs.getString("location")
            )
        } else {
            DatabaseHandler.getConnection()
                    .prepareStatement("INSERT INTO profiles (id) VALUE (?)")
                    .apply { setLong(1, id) }
                    .executeUpdate()

            Profile(
                    id,
                    "A Unifey user.",
                    "",
                    ""
            )
        }
    }


    /**
     * A user's memberships.
     */
    val member by lazy {
        val rs = DatabaseHandler.getConnection()
                .prepareStatement("SELECT * FROM members WHERE id = ?")
                .apply { setLong(1, id) }
                .executeQuery()

        if (rs.next()) {
            val mapper = ObjectMapper()

            Member(
                    id,
                    mapper.readValue(
                            rs.getString("member"),
                            mapper.typeFactory.constructCollectionType(MutableList::class.java, Long::class.java)
                    )
            )
        } else {
            DatabaseHandler.getConnection()
                    .prepareStatement("INSERT INTO members (id, member) VALUE (?, ?)")
                    .apply {
                        setLong(1, id)
                        setString(2, "[]")
                    }
                    .executeUpdate()

            Member(id, mutableListOf())
        }
    }

    fun updateEmail(email: String) {
        this.email = email
    }

    /**
     * A user's global role.
     */
    var role = role
        set(value) {
            DatabaseHandler.getConnection()
                    .prepareStatement("UPDATE users SET role = ? WHERE id = ?")
                    .apply {
                        setInt(1, value)
                        setLong(2, id)
                    }
                    .executeUpdate()

            field = value
        }

    /**
     * A user's unique username. This is also their URL.
     */
    var username = username
        set(value) {
            DatabaseHandler.getConnection()
                    .prepareStatement("UPDATE users SET username = ? WHERE id = ?")
                    .apply {
                        setString(1, value)
                        setLong(2, id)
                    }
                    .executeUpdate()

            field = value
        }

    /**
     * A user's email
     */
    var email = email
        set(value) {
            when {
                !UserManager.EMAIL_REGEX.matches(value) ->
                    throw InvalidArguments("email")

                value.length > 60 ->
                    throw ArgumentTooLarge("email", 60)
            }

            DatabaseHandler.getConnection()
                    .prepareStatement("UPDATE users SET email = ? WHERE id = ?")
                    .apply {
                        setString(1, value)
                        setLong(2, id)
                    }
                    .executeUpdate()

            field = value
        }

    /**
     * A user's password
     */
    private var password = password
        set(value) {
            DatabaseHandler.getConnection()
                    .prepareStatement("UPDATE users SET password = ? WHERE id = ?")
                    .apply {
                        setString(1, value)
                        setLong(2, id)
                    }
                    .executeUpdate()

            field = value
        }
}