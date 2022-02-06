package net.unifey.handle.communities.rules

import kotlinx.serialization.Serializable

/** One of a communities' rules. */
@Serializable data class CommunityRule(val id: Long, var title: String, var body: String)
