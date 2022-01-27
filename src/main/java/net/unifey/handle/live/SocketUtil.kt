package net.unifey.handle.live

import io.ktor.http.cio.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import net.unifey.Unifey
import net.unifey.handle.Error
import net.unifey.handle.live.objs.SocketSession
import net.unifey.handle.live.objs.SocketType

@Serializable
data class SocketResponse<T : Any>(val type: String, val success: Boolean, val response: T)

/** Respond [obj] */
suspend inline fun <reified T : Any> SocketSession.respond(
    type: SocketType,
    success: Boolean,
    obj: T
) {
    session.outgoing.send(
        Frame.Text(Unifey.JSON.encodeToString(SocketResponse(type.toString(), success, obj)))
    )
}

/** Respond [obj], using the inherited type from [SocketSession] */
suspend inline fun <reified T : Any> SocketSession.respond(success: Boolean, obj: T) {
    respond(type, success, obj)
}

@Serializable data class PayloadError(val errorMessage: String)

/** Respond with an [error] */
suspend fun SocketSession.respond(type: SocketType, error: Error) {
    respond(type, false, PayloadError(error.message))
}

/** Respond [obj] successfully. */
suspend inline fun <reified T : Any> SocketSession.respondSuccess(type: SocketType, obj: T) {
    respond(type, true, obj)
}

/** Respond [obj] successfully. */
suspend inline fun <reified T : Any> SocketSession.respondSuccess(obj: T) {
    respond(true, obj)
}

/** Respond [obj] unsuccessfully. */
suspend inline fun <reified T : Any> SocketSession.respondError(type: SocketType, obj: T) {
    respond(type, false, obj)
}

/** Respond [obj] unsuccessfully. */
suspend inline fun <reified T : Any> SocketSession.respondError(obj: T) {
    respond(false, obj)
}
