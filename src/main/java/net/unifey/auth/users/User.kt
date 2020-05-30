package net.unifey.auth.users

data class User(
        var username: String,
        private val password: String,
        private var email: String,
        val uid: Long,
        val createdAt: Long
) {
    fun setEmail(new: String) {
        email = new
    }
}