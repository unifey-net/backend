package net.unifey.handle.communities

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.unifey.handle.NotFound
import net.unifey.handle.communities.rules.CommunityRule
import net.unifey.handle.feeds.FeedManager

/** A community. */
@Serializable
data class Community(
    val id: Long,
    val createdAt: Long,
    val permissions: CommunityPermissions,
    val name: String,
    val description: String,
    val rules: MutableList<CommunityRule>,
    @Transient val roles: MutableMap<Long, Int> = mutableMapOf()
) {
    /** The size of the community. */
    val size
        get() = runBlocking { CommunityManager.getMemberCount(id) }

    /** Get the communities feed. */
    suspend fun getFeed() = FeedManager.getCommunityFeed(this) ?: throw NotFound("community feed")
}
