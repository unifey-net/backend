package net.unifey.handle.feeds.responses

import net.unifey.handle.feeds.Feed

/**
 * The REST response when retrieving feeds.
 */
data class GetFeedResponse(
        val feed: Feed,
        val posts: List<GetPostResponse>,
        val feedPermissions: UserFeedPermissions?
)