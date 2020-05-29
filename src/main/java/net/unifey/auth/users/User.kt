package net.unifey.auth.users

data class User(
        var username: String,
        private val password: String,
        val email: String,
        val uid: Long,
        val createdAt: Long
)