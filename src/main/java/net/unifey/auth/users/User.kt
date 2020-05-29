package net.unifey.auth.users

data class User(
        val username: String,
        private val password: String,
        val email: String,
        val uid: Long,
        val createdAt: Long
)