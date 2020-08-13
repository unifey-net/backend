package net.unifey.handle.communities.rules

/**
 * One of a communities' rules.
 */
data class CommunityRule(
        val id: Long,
        var title: String,
        var body: String
)