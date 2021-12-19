package net.unifey.handle.feeds.posts.comments

data class CommentData(val amount: Int, val pages: Int, val comments: List<GetCommentResponse>)
