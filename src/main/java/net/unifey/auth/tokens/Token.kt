package net.unifey.auth.tokens

import com.fasterxml.jackson.annotation.JsonIgnore
import kotlinx.serialization.Serializable
import net.unifey.handle.users.User
import net.unifey.handle.users.UserManager

/**
 * A token.
 *
 * @param owner The user owner's UID
 * @param token The token string.
 * @param permissions
 * @param expires When the token expires in millis.
 */
@Serializable
data class Token(
    val owner: Long,
    val token: String,
    val permissions: MutableList<String>,
    val expires: Long
) {
    @JsonIgnore
    suspend fun getOwner(): User {
        return UserManager.getUser(owner)
    }
}
