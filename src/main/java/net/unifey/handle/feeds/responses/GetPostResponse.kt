package net.unifey.handle.feeds.responses

import net.unifey.handle.feeds.posts.Post
import net.unifey.handle.users.User

/**
 * The REST response when a post is retrieved
 */
data class GetPostResponse(
        val post: Post,
        val owner: User
)