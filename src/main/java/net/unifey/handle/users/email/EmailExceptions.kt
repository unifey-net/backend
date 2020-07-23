package net.unifey.handle.users.email

import io.ktor.application.call
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import net.unifey.handle.Error
import net.unifey.response.Response

class AlreadyVerified: Error({
    respond(HttpStatusCode.BadRequest, Response("This account has already been verified."))
})

class Unsubscribed: Error({
    respond(HttpStatusCode.BadRequest, Response("An email provided has been unsubscribed from Unifey."))
})

class TooManyAttempts: Error({
    respond(HttpStatusCode.TooManyRequests, Response("Too many email attempts have been requested."))
})

class Unverified: Error({
    respond(HttpStatusCode.BadRequest, Response("You must be verified for this!"))
})