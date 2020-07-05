package net.unifey.auth.tokens

import com.mongodb.client.model.Filters
import net.unifey.handle.mongo.Mongo
import org.bson.Document
import java.util.concurrent.ConcurrentHashMap

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

        val doc = Mongo.getClient()
                .getDatabase("users")
                .getCollection("tokens")
                .find(Filters.eq("token", tokenStr))
                .singleOrNull()

        return if (doc != null) {
            val token = Token(
                    doc.getLong("owner"),
                    doc.getString("token"),
                    doc.getList("permissions", String::class.java),
                    doc.getLong("expires")
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
        val token = Token(owner, tokenStr, mutableListOf(), expire)

        Mongo.getClient()
                .getDatabase("users")
                .getCollection("tokens")
                .insertOne(Document(mapOf(
                        "token" to token.token,
                        "expires" to token.expires,
                        "permissions" to token.permissions,
                        "owner" to token.owner
                )))

        tokenCache[tokenStr] = token

        return token
    }
}