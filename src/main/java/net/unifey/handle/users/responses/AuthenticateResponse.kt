package net.unifey.handle.users.responses

import net.unifey.auth.tokens.Token
import net.unifey.handle.users.User

data class AuthenticateResponse(val token: Token, val user: User)
