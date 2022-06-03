package net.unifey.handle.communities

import kotlinx.serialization.Serializable
import net.unifey.handle.communities.rules.CommunityRule
import net.unifey.handle.feeds.FeedManager

/** A community. */
@Serializable
data class Community(
    val id: Long = -1,
    val createdAt: Long = -1,
    val permissions: CommunityPermissions = CommunityPermissions(0, 0, 0),
    val name: String = "",
    val description: String = "",
    val rules: MutableMap<Long, CommunityRule> = mutableMapOf(),
    val roles: MutableMap<Long, Int> = mutableMapOf()
)

suspend fun Community.getFeed() = FeedManager.getCommunityFeed(this)
