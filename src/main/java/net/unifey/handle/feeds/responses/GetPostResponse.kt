package net.unifey.handle.feeds.responses

import kotlinx.serialization.Serializable
import net.unifey.handle.feeds.posts.Post
import net.unifey.handle.feeds.posts.vote.UserVote
import net.unifey.handle.users.User

/**
 * Getting a post.
 *
 * @param post The post itself.
 * @param author The owner of the post.
 */
@Serializable data class GetPostResponse(val post: Post, val author: User, val vote: UserVote?)
