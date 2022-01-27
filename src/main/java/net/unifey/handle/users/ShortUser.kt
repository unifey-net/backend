package net.unifey.handle.users

data class ShortUser(val username: String, val id: Long) {
    companion object {
        /** Get a [ShortUser] from [user] */
        fun fromUser(user: User): ShortUser = ShortUser(user.username, user.id)

        /** Get a [ShortUser] from [id] */
        suspend fun fromId(id: Long): ShortUser {
            val user = UserManager.getUser(id)

            return fromUser(user)
        }
    }
}
