package net.unifey.handle.communities

object CommunityRoles {
    /**
     * Someone just looking, either signed out or not a member.
     *
     * This is managed internally.
     */
    const val DEFAULT = 0

    /**
     * A member of the community. Someone who's joined.
     *
     * This is managed internally.
     */
    const val MEMBER = 1

    /**
     * A moderator of the community. Delete posts etc
     *
     * This is managed by the administrators and owner.
     */
    const val MODERATOR = 2

    /**
     * An admin of the community. Manage name & desc
     *
     * This is managed by the owner.
     */
    const val ADMIN = 3

    /**
     * The owner cannot be modified, unless by themselves.
     */
    const val OWNER = 4

    fun isValid(role: Int): Boolean =
            (0..4).contains(role)
}