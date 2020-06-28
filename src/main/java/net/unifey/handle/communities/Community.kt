package net.unifey.handle.communities

import com.fasterxml.jackson.databind.ObjectMapper
import net.unifey.DatabaseHandler
import net.unifey.handle.NotFound
import net.unifey.handle.feeds.FeedManager

class Community(
        val id: Long,
        val createdAt: Long,
        postRole: Int,
        viewRole: Int,
        name: String,
        description: String,
        private val roles: HashMap<Long, Int>
) {
    /**
     * The whole where users are allowed to post.
     */
    var postRole = postRole
        set(value) {
            DatabaseHandler.getConnection()
                    .prepareStatement("UPDATE communities SET post_role = ? WHERE id = ?")
                    .apply {
                        setInt(1, value)
                        setLong(2, id)
                    }
                    .executeUpdate()

            field = value
        }

    /**
     * The whole where users are allowed to view the communities' feed.
     */
    var viewRole = viewRole
        set(value) {
            DatabaseHandler.getConnection()
                    .prepareStatement("UPDATE communities SET view_role = ? WHERE id = ?")
                    .apply {
                        setInt(1, value)
                        setLong(2, id)
                    }
                    .executeUpdate()

            field = value
        }

    /**
     * The communities name.
     */
    var name = name
        set(value) {
            DatabaseHandler.getConnection()
                    .prepareStatement("UPDATE communities SET name = ? WHERE id = ?")
                    .apply {
                        setString(1, value)
                        setLong(2, id)
                    }
                    .executeUpdate()

            field = value
        }

    /**
     * The communities description.
     */
    var description = description
        set(value) {
            DatabaseHandler.getConnection()
                    .prepareStatement("UPDATE communities SET description = ? WHERE id = ?")
                    .apply {
                        setString(1, value)
                        setLong(2, id)
                    }
                    .executeUpdate()

            field = value
        }

    /**
     * Update [user]'s role.
     */
    fun setRole(user: Long, role: Int) {
        roles[user] = role

        DatabaseHandler.getConnection()
                .prepareStatement("UPDATE communities SET roles = ? WHERE id = ?")
                .apply {
                    setString(1, ObjectMapper().writeValueAsString(roles))
                    setLong(2, id)
                }
                .executeUpdate()
    }

    /**
     * Get [user]'s role.
     */
    fun getRole(user: Long): Int? =
            roles[user]

    /**
     * Get the communities feed.
     */
    fun getFeed() =
            FeedManager.getCommunityFeed(this) ?:
                    throw NotFound("community feed")
}