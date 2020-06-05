package net.unifey.handle.communities

import com.fasterxml.jackson.databind.ObjectMapper
import net.unifey.DatabaseHandler
import net.unifey.handle.NotFound
import net.unifey.handle.feeds.FeedManager
import net.unifey.util.IdGenerator
import java.sql.ResultSet
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis

/**
 * Manage [Community]s
 *
 * TODO add local cache
 */
object CommunityManager {
    /**
     * Delete a community by it's [id]
     */
    fun deleteCommunity(id: Long) {
        DatabaseHandler.getConnection()
                .prepareStatement("DELETE FROM communities WHERE id = ?")
                .apply { setLong(1, id) }
                .executeUpdate()
    }

    /**
     * Get a [Community] by [name].
     */
    fun getCommunity(name: String): Community {
        val rs = DatabaseHandler.getConnection()
                .prepareStatement("SELECT * FROM communities WHERE name = ?")
                .apply { setString(1, name) }
                .executeQuery()

        if (rs.next())
            return getCommunity(rs)
        else throw NotFound("community")
    }

    /**
     * Get a [Community] by [id].
     */
    fun getCommunity(id: Long): Community {
        val rs = DatabaseHandler.getConnection()
                .prepareStatement("SELECT * FROM communities WHERE id = ?")
                .apply { setLong(1, id) }
                .executeQuery()

        if (rs.next())
            return getCommunity(rs)
        else throw NotFound("community")
    }

    /**
     * Parse a [Community] from [rs]. Make sure you .next before this.
     */
    private fun getCommunity(rs: ResultSet): Community {
        val mapper = ObjectMapper()

        return Community(
                rs.getLong("id"),
                rs.getLong("created_at"),
                rs.getInt("post_role"),
                rs.getString("name"),
                rs.getString("description"),
                mapper.readValue(rs.getString("roles"), mapper.typeFactory.constructMapType(
                        HashMap::class.java,
                        Long::class.java,
                        Int::class.java
                ))
        )
    }

    /**
     * Get communities
     */
    fun getCommunities(limit: Int = 100, startAt: Int = 0): List<Community> {
        val rs = DatabaseHandler.getConnection()
                .prepareStatement("SELECT * FROM communities LIMIT ?, ?")
                .apply {
                    setInt(1, startAt)
                    setInt(2, limit + startAt)
                }
                .executeQuery()

        val communities = mutableListOf<Community>()

        while (rs.next()) {
            communities.add(getCommunity(rs))
        }

        return communities
    }

    /**
     * TODO If [user] can create a community.
     */
    fun canCreate(user: Long): Boolean {
        return true
    }

    /**
     * Create a community.
     */
    fun createCommunity(owner: Long, name: String, desc: String): Community {
        val roles = hashMapOf(owner to CommunityRoles.OWNER)

        val community = Community(
                IdGenerator.getId(),
                System.currentTimeMillis(),
                CommunityRoles.MEMBER,
                name,
                desc,
                roles
        )

        DatabaseHandler.getConnection()
                .prepareStatement("INSERT INTO communities (name, created_at, description, id, roles, post_role) VALUES (?, ?, ?, ?, ?, ?)")
                .apply {
                    setString(1, community.name)
                    setLong(2, community.createdAt)
                    setString(3, community.description)
                    setLong(4, community.id)
                    setString(5, ObjectMapper().writeValueAsString(roles))
                    setInt(6, community.postRole)
                }
                .executeUpdate()

        FeedManager.createFeedForCommunity(community.id, owner)

        return community
    }
}