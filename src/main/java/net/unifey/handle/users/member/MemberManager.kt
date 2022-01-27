package net.unifey.handle.users.member

import net.unifey.handle.NotFound
import net.unifey.handle.mongo.MONGO
import net.unifey.handle.users.User
import org.litote.kmongo.eq
import org.litote.kmongo.pull
import org.litote.kmongo.push

/** Manages [User]'s joined communities, and which ones they're receiving notifications from. */
object MemberManager {
    /** [id] joins [community] */
    suspend fun joinCommunity(id: Long, community: Long) {
        MONGO
            .getDatabase("users")
            .getCollection<Member>("members")
            .updateOne(Member::id eq id, push(Member::member, community))
    }

    /** user joins [community]. */
    suspend fun User.joinCommunity(community: Long) {
        joinCommunity(id, community)
    }

    /** [id] leaves [community] */
    suspend fun leaveCommunity(id: Long, community: Long) {
        MONGO
            .getDatabase("users")
            .getCollection<Member>("members")
            .updateOne(Member::id eq id, pull(Member::member, community))
    }

    /** user leaves [community]. */
    suspend fun User.leaveCommunity(community: Long) {
        leaveCommunity(id, community)
    }

    /** [id] joins [community] notifications */
    suspend fun joinNotifications(id: Long, community: Long) {
        MONGO
            .getDatabase("users")
            .getCollection<Member>("members")
            .updateOne(Member::id eq id, push(Member::notifications, community))
    }

    /** [User] joins [community] notifications */
    suspend fun User.joinNotifications(community: Long) {
        joinNotifications(id, community)
    }

    /** [User] leaves [community] notifications */
    suspend fun User.leaveNotifications(community: Long) {
        leaveNotifications(id, community)
    }

    /** [id] leaves [community] notifications */
    suspend fun leaveNotifications(id: Long, community: Long) {
        MONGO
            .getDatabase("users")
            .getCollection<Member>("members")
            .updateOne(Member::id eq id, pull(Member::notifications, community))
    }

    /** Get [id]'s membership. */
    @Throws(NotFound::class)
    suspend fun getMember(id: Long): Member {
        return MONGO.getDatabase("users").getCollection<Member>("members").findOne(Member::id eq id)
            ?: throw NotFound("member")
    }

    /** Get [User]'s member. */
    @Throws(NotFound::class) suspend fun User.getMember(): Member = getMember(id)

    /** Check if [User] is member of [community]. */
    suspend fun User.isMemberOf(community: Long): Boolean = getMember(id).member.contains(community)

    /** Check if [User] has notifications on for [community]. */
    suspend fun User.hasNotificationsOn(community: Long): Boolean =
        getMember(id).notifications.contains(community)
}
