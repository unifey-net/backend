package net.unifey.handle.users.profile

import net.unifey.DatabaseHandler

/**
 * A user's profile.
 */
class Profile(
        val id: Long,
        description: String,
        discord: String,
        location: String
) {
    /**
     * A user's Discord.
     *
     * TODO verify this using Discord
     */
    var discord = discord
        set(value) {
            DatabaseHandler.getConnection()
                    .prepareStatement("UPDATE profiles SET discord = ? WHERE id = ?")
                    .apply {
                        setString(1, value)
                        setLong(2, id)
                    }
                    .executeUpdate()

            field = value
        }

    /**
     * A user's description.
     */
    var description = description
        set(value) {
            DatabaseHandler.getConnection()
                    .prepareStatement("UPDATE profiles SET description = ? WHERE id = ?")
                    .apply {
                        setString(1, value)
                        setLong(2, id)
                    }
                    .executeUpdate()

            field = value
        }

    /**
     * A user's location. This is adjustable by user, and is not accurate.
     */
    var location = location
        set(value) {
            DatabaseHandler.getConnection()
                    .prepareStatement("UPDATE profiles SET location = ? WHERE id = ?")
                    .apply {
                        setString(1, value)
                        setLong(2, id)
                    }
                    .executeUpdate()

            field = value
        }
}