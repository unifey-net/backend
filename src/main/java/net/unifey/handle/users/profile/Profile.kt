package net.unifey.handle.users.profile

import kotlinx.serialization.Serializable

/** A user's profile. */
@Serializable
data class Profile(
    val id: Long,
    val description: String,
    val discord: String,
    val location: String
) {
    companion object {
        const val MAX_DESC_LEN = 256
        const val MAX_LOC_LEN = 32
        const val MAX_DISC_LEN = 64
    }
}
