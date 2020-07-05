package net.unifey.handle.users.member

import com.fasterxml.jackson.annotation.JsonIgnore
import com.mongodb.client.model.Filters
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
     *
     * TODO check if already in
     */
    fun join(community: Long) {
        member.add(community)

        update()
    }

    /**
     * Leave a [community]
     *
     * TODO check if in
     */
    fun leave(community: Long) {
        member.remove(community)

        update()
    }

    /**
     * Update [member] in the database.
     */
    private fun update() {
        Mongo.getClient()
                .getDatabase("users")
                .getCollection("members")
                .updateOne(Filters.eq("id", id), Document(mapOf(
                        "member" to member
                )))
    }
}