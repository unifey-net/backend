package net.unifey.handle.feeds.posts.comments

import kotlinx.serialization.Serializable

@Serializable
data class CommentData(val amount: Int, val pages: Int, val comments: List<GetCommentResponse>)
