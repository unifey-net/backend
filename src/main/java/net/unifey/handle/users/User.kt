package net.unifey.handle.users

import net.unifey.DatabaseHandler
import net.unifey.handle.users.profile.Profile

class User(
        val id: Long,
        username: String,
        password: String,
        email: String,
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

    fun updateEmail(email: String) {
        this.email = email
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
    private var email = email
        set(value) {
            when {
                !UserManager.EMAIL_REGEX.matches(value) ->
                    throw InvalidInput("Invalid email!")

                value.length > 60 ->
                    throw InvalidInput("Your email must be under 60 characters.")
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