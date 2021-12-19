package net.unifey.handle.users

data class ShortUser(val username: String, val id: Long) {
    companion object {
        /** Get a [ShortUser] from [user] */
        fun fromUser(user: User): ShortUser = ShortUser(user.username, user.id)
    }
}
