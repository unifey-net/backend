package net.unifey.handle.users.member

import com.fasterxml.jackson.annotation.JsonIgnore
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import net.unifey.handle.AlreadyExists
import net.unifey.handle.NotFound
import net.unifey.handle.communities.CommunityManager
import net.unifey.handle.communities.CommunityRoles
import net.unifey.handle.mongo.Mongo
import org.bson.Document

class Member(
        @JsonIgnore
        val id: Long,
        private val member: MutableList<Long>
) {
    /**
     * If user is a member of [community].
     */
    fun isMemberOf(community: Long): Boolean =
            member.contains(community)

    /**
     * Get the membership to communities
     */
    fun getMembers(): List<Long> =
            member

    /**
     * Join a [community]
     */
    fun join(community: Long) {
        if (member.contains(community))
            throw AlreadyExists("community", community.toString())

        member.add(community)

        update()
    }

    /**
     * Leave a [community]
     */
    fun leave(community: Long) {
        if (!member.contains(community))
            throw NotFound("community")

        member.remove(community)

        CommunityManager
                .getCommunityById(community)
                .removeRole(id)

        update()
    }

    /**
     * Update [member] in the database.
     */
    private fun update() {
        Mongo.getClient()
                .getDatabase("users")
                .getCollection("members")
                .updateOne(Filters.eq("id", id), Updates.set("member", member))
    }
}