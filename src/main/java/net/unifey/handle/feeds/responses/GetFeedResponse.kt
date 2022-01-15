package net.unifey.handle.feeds.responses

import kotlinx.serialization.Serializable
import net.unifey.handle.feeds.Feed

/** The REST response when retrieving feeds. */
@Serializable
data class GetFeedResponse(
    val feed: Feed,
    val posts: List<GetPostResponse>,
    val feedPermissions: UserFeedPermissions?
)
