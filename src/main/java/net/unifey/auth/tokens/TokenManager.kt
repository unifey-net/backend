package net.unifey.auth.tokens

import com.mongodb.client.model.Filters
import io.ktor.client.request.forms.*
import java.util.concurrent.ConcurrentHashMap
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.User
import org.bson.Document

object TokenManager {
    /**
     * A cache of tokens.
     *
     * Key: Token identifier
     */
    private val tokenCache = ConcurrentHashMap<String, Token>()

    /** Get a token by their [tokenStr]. Prefers [tokenCache] over database. */
    fun getToken(tokenStr: String): Token? {
        if (tokenCache.containsKey(tokenStr)) return tokenCache[tokenStr]!!

        val doc =
            Mongo.getClient()
                .getDatabase("users")
                .getCollection("tokens")
                .find(Filters.eq("token", tokenStr))
                .singleOrNull()

        return if (doc != null) {
            val token = formToken(doc)

            tokenCache[tokenStr] = token

            token
        } else null
    }

    /** If [token] has expired */
    fun isTokenExpired(token: Token): Boolean =
        token.expires != -1L && System.currentTimeMillis() > token.expires

    /** Create a token with [tokenStr], [owner] and [expire]. */
    fun createToken(tokenStr: String, owner: Long, expire: Long): Token {
        val token = Token(owner, tokenStr, mutableListOf(), expire)

        Mongo.getClient()
            .getDatabase("users")
            .getCollection("tokens")
            .insertOne(
                Document(
                    mapOf(
                        "token" to token.token,
                        "expires" to token.expires,
                        "permissions" to token.permissions,
                        "owner" to token.owner
                    )
                )
            )

        tokenCache[tokenStr] = token

        return token
    }

    /** Form a [Token] from a [doc] */
    private fun formToken(doc: Document): Token {
        return Token(
            doc.getLong("owner"),
            doc.getString("token"),
            doc.getList("permissions", String::class.java),
            doc.getLong("expires")
        )
    }

    /** Get all active tokens for [user] */
    fun getTokensForUser(user: User): List<Token> {
        val tokens =
            Mongo.getClient()
                .getDatabase("users")
                .getCollection("tokens")
                .find(Filters.eq("owner", user.id))
                .filter { doc ->
                    doc.getLong("expires") == -1L ||
                        System.currentTimeMillis() > doc.getLong("expires")
                }

        return tokens.map(::formToken)
    }

    /** Delete [token] */
    fun deleteToken(token: String) {
        Mongo.getClient()
            .getDatabase("users")
            .getCollection("tokens")
            .deleteOne(Filters.eq("token", token))
    }

    /** Delete [token] */
    fun deleteToken(token: Token) {
        deleteToken(token.token)
    }
}
