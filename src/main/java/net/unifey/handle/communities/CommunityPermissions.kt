package net.unifey.handle.communities

import kotlinx.serialization.Serializable

/** The permissions of a community. */
@Serializable
data class CommunityPermissions(
    val postRole: Int,
    val viewRole: Int,
    val commentRole: Int,
)
