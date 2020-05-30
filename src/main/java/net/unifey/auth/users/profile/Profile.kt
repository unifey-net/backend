package net.unifey.auth.users.profile

data class Profile(
        val id: Long,
        val description: String,
        val discord: String,
        val location: String
) {
    companion object {
        val CHANGEABLE_KEYS = listOf("description", "discord", "location")
    }
}