package net.unifey.handle.feeds.posts.comments

import kotlinx.serialization.Serializable
import net.unifey.handle.feeds.posts.vote.UserVote
import net.unifey.handle.users.User

@Serializable
data class GetCommentResponse(
    val comment: Comment,
    val comments: CommentData?,
    val vote: UserVote?,
    val author: User
)
