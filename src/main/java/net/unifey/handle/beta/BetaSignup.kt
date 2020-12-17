package net.unifey.handle.beta

data class BetaSignup(
        val username: String,
        val email: String,
        val date: Long,
        var verified: Boolean
)