package net.unifey.auth.tokens

/**
 * A token.
 *
 * @param owner The user owner's UID
 * @param token The token string.
 * @param permissions The JSONArray of permissions. (open to change)
 * @param expires When the token expires in millis.
 */
data class Token(
        val owner: Long,
        val token: String,
        val permissions: String,
        val expires: Long
)