package net.unifey.handle.communities.responses

import kotlinx.serialization.Serializable
import net.unifey.handle.communities.Community
import net.unifey.handle.emotes.Emote
import net.unifey.handle.feeds.Feed

/**
 * Get a community.
 *
 * @param community The community.
 * @param selfRole The requesting user's role in the community. null if they're not signed in.
 * @param emotes The global and community emotes.
 * @param feed The feed of the community. Used to view the page count.
 */
@Serializable
data class GetCommunityResponse(
    val community: Community,
    val selfRole: Int?,
    val emotes: List<Emote>?,
    val feed: Feed?
)
