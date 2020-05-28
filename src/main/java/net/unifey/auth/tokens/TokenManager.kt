package net.unifey.auth.tokens

import net.unifey.DatabaseHandler
import java.util.concurrent.ConcurrentHashMap
import javax.xml.crypto.Data

object TokenManager {
    /**
     * A cache of tokens.
     *
     * Key: Token identifier
     */
    private val tokenCache = ConcurrentHashMap<String, Token>()

    /**
     * Get a token by their [tokenStr]. Prefers [tokenCache] over database.
     */
    fun getToken(tokenStr: String): Token? {
        if (tokenCache.containsKey(tokenStr))
            return tokenCache[tokenStr]!!

        val rs = DatabaseHandler.getConnection()
                .prepareStatement("SELECT token, expires, permissions, owner FROM tokens WHERE token = ?")
                .apply { setString(1, tokenStr) }
                .executeQuery()

        return if (rs.next()) {
            val token = Token(
                    rs.getLong("owner"),
                    rs.getString("token"),
                    rs.getString("permissions"),
                    rs.getLong("expires")
            )

            tokenCache[tokenStr] = token

            token
        } else null
    }

    /**
     * If [token] has expired
     */
    fun isTokenExpired(token: Token): Boolean =
            System.currentTimeMillis() > token.expires

    /**
     * Create a token with [tokenStr], [owner] and [expire].
     */
    fun createToken(tokenStr: String, owner: Long, expire: Long): Token {
        val token = Token(owner, tokenStr, "", expire)

        DatabaseHandler.getConnection()
                .prepareStatement("INSERT INTO tokens (token, expires, permissions, owner) VALUES (?, ?, ?, ?)")
                .apply {
                    setString(1, token.token)
                    setLong(2, token.expires)
                    setString(3, token.permissions)
                    setLong(4, token.owner)
                }
                .executeUpdate()

        tokenCache[tokenStr] = token

        return token
    }
}