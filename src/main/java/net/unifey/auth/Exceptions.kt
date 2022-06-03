package net.unifey.auth

import io.ktor.http.*
import io.ktor.server.response.*
import net.unifey.handle.Error
import net.unifey.response.Response

open class AuthenticationException(message: String) :
    Error({ respond(HttpStatusCode.Unauthorized, Response(message)) })

class TokenExpiredException : AuthenticationException("Token is expired!")
