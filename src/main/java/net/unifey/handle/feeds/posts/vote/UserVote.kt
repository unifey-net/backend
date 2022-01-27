package net.unifey.handle.feeds.posts.vote

import kotlinx.serialization.Serializable

@Serializable
data class UserVote(val vote: Int, val id: Long, val user: Long, val type: Int) {
    companion object {
        const val COMMENT = 1
        const val POST = 2
    }
}
