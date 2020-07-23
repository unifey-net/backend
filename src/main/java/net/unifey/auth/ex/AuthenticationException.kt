package net.unifey.auth.ex

import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import net.unifey.handle.Error
import net.unifey.response.Response

open class AuthenticationException(message: String): Error({
    respond(HttpStatusCode.Unauthorized, Response(message))
})