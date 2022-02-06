package net.unifey.handle.users

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** A user. */
@Serializable
data class User(
    val id: Long,
    val username: String,
    @Transient val password: String = "",
    @Transient val email: String = "",
    val verified: Boolean,
    val role: Int,
    val createdAt: Long
)
