package net.unifey.handle.users.member

import com.fasterxml.jackson.annotation.JsonIgnore
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import net.unifey.handle.AlreadyExists
import net.unifey.handle.NoPermission
import net.unifey.handle.NotFound
import net.unifey.handle.communities.CommunityManager
import net.unifey.handle.mongo.Mongo

/**
 * @param id The user's ID.
 * @param member The communities the user's in.
 * @param notifications The communities the user has notifications on for.
 */
class Member(
    @JsonIgnore val id: Long,
    private val member: MutableList<Long>,
    private val notifications: MutableList<Long>
) {
    /** If user is a member of [community]. */
    fun isMemberOf(community: Long): Boolean = member.contains(community)

    /** Get the membership to communities */
    fun getMembers(): List<Long> = member

    /** Join a [community] */
    fun join(community: Long) {
        if (member.contains(community)) throw AlreadyExists("community", community.toString())

        member.add(community)

        update()
    }

    /** Leave a [community] */
    fun leave(community: Long) {
        if (!member.contains(community)) throw NotFound("community")

        member.remove(community)
        notifications.remove(community)

        CommunityManager.getCommunityById(community).removeRole(id)

        update()
    }

    /** Enable notifications for [community]. They must be joined. */
    fun enableNotifications(community: Long) {
        if (!member.contains(community)) throw NoPermission()

        notifications.add(community)
    }

    /** Disable notifications for [community]. */
    fun disableNotifications(community: Long) {
        if (!notifications.contains(community)) throw NotFound("community")

        notifications.remove(community)
    }

    /** Get the notification subscribed communities */
    fun getNotifications(): List<Long> = notifications

    /** If the user has notifications enabled for [community]. */
    fun hasNotificationsEnabled(community: Long): Boolean = getNotifications().contains(community)

    /** Update [member] in the database. */
    private fun update() {
        Mongo.getClient()
            .getDatabase("users")
            .getCollection("members")
            .updateOne(Filters.eq("id", id), Updates.set("member", member))
    }
}
