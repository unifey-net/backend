package net.unifey.handle.communities.responses

import net.unifey.handle.communities.Community
import net.unifey.handle.feeds.responses.GetFeedResponse

data class GetCommunityResponse(
        val community: Community,
        val selfRole: Int?,
        val feed: GetFeedResponse?
)