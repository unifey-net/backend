package net.unifey.handle.feeds.responses

import net.unifey.handle.feeds.posts.Post
import net.unifey.handle.feeds.posts.vote.UserVote
import net.unifey.handle.users.User

/**
 * Getting a post.
 *
 * @param post The post itself.
 * @param owner The owner of the post.
 */
data class GetPostResponse(
        val post: Post,
        val owner: User,
        val vote: UserVote?
)