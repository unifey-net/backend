package net.unifey.handle.users.email

import io.ktor.application.call
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import net.unifey.response.Response

fun StatusPages.Configuration.registerEmailExceptions() {
    exception<TooManyAttempts> {
        call.respond(HttpStatusCode.TooManyRequests, Response("Too many email attempts have been requested."))
    }

    exception<Unsubscribed> {
        call.respond(HttpStatusCode.BadRequest, Response("An email provided has unsubscribed from Unifey."))
    }

    exception<AlreadyVerified> {
        call.respond(HttpStatusCode.BadRequest, Response("This account has already been verified."))
    }

    exception<Unverified> {
        call.respond(HttpStatusCode.BadRequest, Response("You cannot do this while unverified!"))
    }
}

class AlreadyVerified: Throwable()

class Unsubscribed: Throwable()

class TooManyAttempts: Throwable()

class Unverified: Throwable()