package net.unifey.handle.users.email

import io.ktor.http.*
import io.ktor.response.*
import net.unifey.handle.Error
import net.unifey.response.Response

/** The account's been already verified. */
class AlreadyVerified :
    Error({
        respond(HttpStatusCode.BadRequest, Response("This account has already been verified."))
    })

/** The email has been unsubscribed and can't be used. */
class Unsubscribed :
    Error({
        respond(
            HttpStatusCode.BadRequest,
            Response("An email provided has been unsubscribed from Unifey.")
        )
    })

/** A resend email has been used to many times. */
class TooManyAttempts :
    Error({
        respond(
            HttpStatusCode.TooManyRequests,
            Response("Too many email attempts have been requested.")
        )
    })

/** An action that requires verification. */
class Unverified :
    Error({ respond(HttpStatusCode.BadRequest, Response("You must be verified for this!")) })
