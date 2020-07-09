package net.unifey.handle.feeds.responses

import net.unifey.handle.emotes.Emote
import net.unifey.handle.feeds.Feed
import net.unifey.response.Response

/**
 * The REST response when retrieving feeds.
 */
data class GetFeedResponse(
        val feed: Feed,
        val posts: List<GetPostResponse>
)