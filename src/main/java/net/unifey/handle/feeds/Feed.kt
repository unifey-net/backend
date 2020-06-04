package net.unifey.handle.feeds

import com.fasterxml.jackson.databind.ObjectMapper
import net.unifey.DatabaseHandler

class Feed(
        val id: String,
        val banned: MutableList<Long>,
        val moderators: MutableList<Long>
) {
    /**
     * Update this feeds data to the database
     */
    fun update() {
        DatabaseHandler.getConnection()
                .prepareStatement("UPDATE feeds SET banned = ?, moderators = ? WHERE id = ?")
                .apply {
                    val mapper = ObjectMapper()

                    setString(1, mapper.writeValueAsString(banned))
                    setString(2, mapper.writeValueAsString(moderators))
                    setString(3, id)
                }
                .executeUpdate()
    }
}