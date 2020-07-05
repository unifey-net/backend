package net.unifey.handle.communities.responses

import net.unifey.handle.communities.Community
import net.unifey.handle.feeds.Feed
import net.unifey.handle.feeds.responses.GetFeedResponse

/**
 * Get a community.
 *
 * @param community The community.
 * @param selfRole The requesting user's role in the community. null if they're not signed in.
 * @param feed The feed of the community. Used to view the page count.
 */
data class GetCommunityResponse(
        val community: Community,
        val selfRole: Int?,
        val feed: Feed?
)