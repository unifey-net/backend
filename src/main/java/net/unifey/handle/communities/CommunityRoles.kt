package net.unifey.handle.communities

object CommunityRoles {
    /**
     * Someone just looking, either signed out or not a member.
     */
    const val DEFAULT = 0

    /**
     * A member of the community. Someone who's joined.
     */
    const val MEMBER = 1

    /**
     * A moderator of the community. Delete posts etc
     */
    const val MODERATOR = 2

    /**
     * An admin of the community. Manage name & desc
     */
    const val ADMIN = 3

    /**
     * The owner cannot be modified, unless by themselves.
     */
    const val OWNER = 4
}