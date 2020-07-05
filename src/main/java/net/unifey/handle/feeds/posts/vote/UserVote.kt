package net.unifey.handle.feeds.posts.vote

data class UserVote(
        val vote: Int,
        val post: Long,
        val user: Long
)