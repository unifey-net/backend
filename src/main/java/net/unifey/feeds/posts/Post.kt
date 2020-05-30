package net.unifey.feeds.posts

data class Post(
        val id: Long,
        val createdAt: Long,
        val authorUid: Long,
        val title: String,
        val content: String,
        val feed: String,
        val hidden: Boolean,
        val upvotes: Long,
        val downvotes: Long
)