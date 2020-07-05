package net.unifey.handle.communities

import com.mongodb.client.model.Filters
import net.unifey.handle.NotFound
import net.unifey.handle.feeds.FeedManager
import net.unifey.handle.mongo.Mongo
import org.bson.Document

class Community(
        val id: Long,
        val createdAt: Long,
        postRole: Int,
        viewRole: Int,
        name: String,
        description: String,
        private val roles: MutableMap<Long, Int>
) {
    /**
     * The whole where users are allowed to post.
     */
    var postRole = postRole
        set(value) {
            Mongo.getClient()
                    .getDatabase("communities")
                    .getCollection("communities")
                    .updateOne(Filters.eq("id", id), Document(mapOf(
                            "permissions" to Document(mapOf(
                                    "post_role" to value
                            ))
                    )))

            field = value
        }

    /**
     * The whole where users are allowed to view the communities' feed.
     */
    var viewRole = viewRole
        set(value) {
            Mongo.getClient()
                    .getDatabase("communities")
                    .getCollection("communities")
                    .updateOne(Filters.eq("id", id), Document(mapOf(
                            "permissions" to Document(mapOf(
                                    "view_role" to value
                            ))
                    )))

            field = value
        }

    /**
     * The communities name.
     */
    var name = name
        set(value) {
            Mongo.getClient()
                    .getDatabase("communities")
                    .getCollection("communities")
                    .updateOne(Filters.eq("id", id), Document(mapOf(
                            "name" to value
                    )))

            field = value
        }

    /**
     * The communities description.
     */
    var description = description
        set(value) {
            Mongo.getClient()
                    .getDatabase("communities")
                    .getCollection("communities")
                    .updateOne(Filters.eq("id", id), Document(mapOf(
                            "description" to value
                    )))

            field = value
        }

    /**
     * Update [user]'s role.
     */
    fun setRole(user: Long, role: Int) {
        roles[user] = role

        Mongo.getClient()
                .getDatabase("communities")
                .getCollection("communities")
                .updateOne(Filters.eq("id", id), Document(mapOf(
                        "roles" to Document(mapOf(
                                "$user" to role
                        ))
                )))
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