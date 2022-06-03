package net.unifey.auth.tokens

import java.util.concurrent.ConcurrentHashMap
import net.unifey.handle.mongo.MONGO
import net.unifey.handle.users.User
import org.litote.kmongo.*

object TokenManager {
    /**
     * A cache of tokens.
     *
     * Key: Token identifier
     */
    private val tokenCache = ConcurrentHashMap<String, Token>()

    /** Get a token by their [tokenStr]. Prefers [tokenCache] over database. */
    suspend fun getToken(tokenStr: String): Token? {
        if (tokenCache.containsKey(tokenStr)) return tokenCache[tokenStr]!!

        val token =
            MONGO
                .getDatabase("users")
                .getCollection<Token>("tokens")
                .findOne(Token::token eq tokenStr)

        return if (token != null) {
            tokenCache[tokenStr] = token

            token
        } else null
    }

    /** If [token] has expired */
    fun isTokenExpired(token: Token): Boolean =
        token.expires != -1L && System.currentTimeMillis() > token.expires

    /** Create a token with [tokenStr], [owner] and [expire]. */
    suspend fun createToken(tokenStr: String, owner: Long, expire: Long): Token {
        val token = Token(owner, tokenStr, mutableListOf(), expire)

        MONGO.getDatabase("users").getCollection<Token>("tokens").insertOne(token)

        tokenCache[tokenStr] = token

        return token
    }

    /** Get all active tokens for [user] */
    suspend fun getTokensForUser(user: User): List<Token> {
        return MONGO
            .getDatabase("users")
            .getCollection<Token>("tokens")
            .find(
                and(
                    Token::owner eq user.id,
                    Token::expires ne -1,
                    Token::expires lt System.currentTimeMillis()
                )
            )
            .toList()
    }

    /** Delete [token] */
    suspend fun deleteToken(token: String) {
        MONGO.getDatabase("users").getCollection<Token>("tokens").deleteOne(Token::token eq token)
    }

    /** Delete [token] */
    suspend fun deleteToken(token: Token) {
        deleteToken(token.token)
    }
}
