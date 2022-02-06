package net.unifey.handle

import dev.ajkneisl.lib.util.eitherOr
import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import net.unifey.response.Response

/** [obj] was not found. */
class NotFound(private val obj: String = "") :
    Error(
        {
            respond(
                HttpStatusCode.BadRequest,
                Response(
                    (obj == "").eitherOr("That could not be found", "That $obj could not be found")
                )
            )
        },
        (obj == "").eitherOr("That could not be found", "That $obj could not be found")
    )

/** Required [args] for making request. */
class InvalidArguments(private vararg val args: String) :
    Error(
        {
            respond(
                HttpStatusCode.BadRequest,
                Response("Required arguments: ${args.joinToString(", ")}")
            )
        },
        "Required arguments: ${args.joinToString(", ")}"
    )

/** [arg] is too large. (over [max]) */
class ArgumentTooLarge(private val arg: String, private val max: Int) :
    Error({ respond(HttpStatusCode.BadRequest, Response("$arg must be under $max.")) })

/** [type] with [arg] already exists. */
class AlreadyExists(val type: String, private val arg: String) :
    Error({
        respond(HttpStatusCode.BadRequest, Response("A $type with that $arg already exists!"))
    })

/** User doesn't have permission. */
class NoPermission :
    Error(
        { respond(HttpStatusCode.Unauthorized, Response("You don't have permission for this")) },
        "You don't have permission for this"
    )

/** An invalid input for an input the user decides. */
class InvalidVariableInput(val type: String, val issue: String) :
    Error({ respond(HttpStatusCode.BadRequest, Response("${type.capitalize()}: $issue")) })

/** An invalid type */
class InvalidType : Error({ respond(HttpStatusCode.BadRequest, Response("Invalid type!")) })

/** The body is too large. */
class BodyTooLarge :
    Error({ respond(HttpStatusCode.PayloadTooLarge, Response("The body is too large.")) })

/** The limit of something has been reached. */
class LimitReached :
    Error({ respond(HttpStatusCode.InsufficientStorage, Response("Limit has been reached.")) })

/** Internal content is malformed. */
class MalformedContent :
    Error({
        respond(HttpStatusCode.InternalServerError, "There was an issue with internal content.")
    })

/**
 * An error.
 * @param response For a regular REST call
 * @param message Optional websocket support.
 */
open class Error(
    val response: suspend ApplicationCall.() -> Unit,
    override val message: String = "There was an issue with that request!"
) : Throwable()
