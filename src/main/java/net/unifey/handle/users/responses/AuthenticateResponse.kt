package net.unifey.handle.users.responses

import kotlinx.serialization.Serializable
import net.unifey.auth.tokens.Token
import net.unifey.handle.users.User

@Serializable data class AuthenticateResponse(val token: Token, val user: User)
