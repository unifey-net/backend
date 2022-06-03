package net.unifey.auth

import io.ktor.server.application.*
import io.ktor.server.auth.*
import net.unifey.auth.tokens.Token
import net.unifey.auth.tokens.TokenManager
import net.unifey.handle.users.GlobalRoles
import net.unifey.util.DEFAULT_PAGE_RATE_LIMIT
import net.unifey.util.PageRateLimit
import net.unifey.util.checkRateLimit

/** Check if an [ApplicationCall] is authenticated. */
suspend fun ApplicationCall.isAuthenticated(
    rateLimit: Boolean = true,
    pageRateLimit: PageRateLimit = DEFAULT_PAGE_RATE_LIMIT,
    permissionLevel: Int = GlobalRoles.DEFAULT
): Token {
    val token = getTokenFromCall()

    when {
        TokenManager.isTokenExpired(token) -> throw TokenExpiredException()
        rateLimit -> checkRateLimit(token, pageRateLimit)
        permissionLevel != GlobalRoles.DEFAULT -> permissionLevel >= token.getOwner().role
    }

    return token
}

/** Get a [Token] from an [ApplicationCall] */
suspend fun ApplicationCall.getTokenFromCall(): Token {
    val header = getHeader(this) ?: throw AuthenticationException("No authentication")

    when (header.first.lowercase()) {
        "bearer" -> return TokenManager.getToken(header.second)
                ?: throw AuthenticationException("Invalid token")
        else -> throw AuthenticationException("Use bearer token")
    }
}

/** Turn the [ApplicationCall]'s authorization header into a pair. It is the type and token. */
internal fun getHeader(call: ApplicationCall): Pair<String, String>? {
    val header = call.request.parseAuthorizationHeader()?.render()?.split(" ") ?: return null

    return Pair(header[0], header[1])
}
