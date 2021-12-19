package net.unifey.handle.feeds.custom.response

import net.unifey.handle.feeds.custom.LimitedFeed
import net.unifey.handle.feeds.responses.GetPostResponse

data class GetLimitedFeedResponse(val posts: MutableList<GetPostResponse>, val feed: LimitedFeed)
