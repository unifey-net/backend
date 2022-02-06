package net.unifey.handle.feeds.posts

import kotlinx.serialization.Serializable

/**
 * A post.
 *
 * @param id The ID of the post.
 * @param createdAt When the post was created.
 * @param authorId The ID of the user who created the post.
 * @param feed The ID of the feed the post was created in.
 */
@Serializable
data class Post(
    val id: Long,
    val createdAt: Long,
    val authorId: Long,
    val feed: String,
    val title: String,
    val content: String,
    val upVotes: Long,
    val downVotes: Long,
    val hidden: Boolean,
    val pinned: Boolean,
    val edited: Boolean,
    val nsfw: Boolean
)
