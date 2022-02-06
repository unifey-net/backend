package net.unifey.handle.feeds.posts

import io.ktor.application.ApplicationCall
import net.unifey.auth.tokens.Token
import net.unifey.handle.InvalidArguments
import net.unifey.handle.NoPermission
import net.unifey.handle.feeds.Feed
import net.unifey.handle.feeds.FeedManager
import net.unifey.handle.feeds.getFeed

@Throws(NoPermission::class)
suspend fun ApplicationCall.getPost(
    requirePost: Boolean = false,
    requireComment: Boolean = false
): Triple<Token?, Post, Feed> {
    val (feed, token) =
        getFeed(requireView = true, requirePost = requirePost, requireComment = requireComment)

    val id = parameters["post"]?.toLongOrNull() ?: throw InvalidArguments("post")

    val post = FeedManager.getPost(id)

    return Triple(token, post, feed)
}
