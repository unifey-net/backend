package net.unifey.auth.users

data class User(
        val username: String,
        val password: String,
        val email: String,
        val uid: Long,
        val createdAt: String
)