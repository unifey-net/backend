package net.unifey.handle.users.member

import com.fasterxml.jackson.databind.ObjectMapper
import net.unifey.DatabaseHandler

class Member(
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
        DatabaseHandler.getConnection()
                .prepareStatement("UPDATE members SET member = ? WHERE id = ?")
                .apply {
                    setString(1, ObjectMapper().writeValueAsString(member))
                    setLong(2, id)
                }
                .executeUpdate()
    }
}