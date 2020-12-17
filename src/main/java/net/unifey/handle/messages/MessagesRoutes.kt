package net.unifey.handle.messages

import io.ktor.request.receiveParameters
import io.ktor.routing.Routing
import io.ktor.websocket.webSocket

fun Routing.messageRoutes() {
    webSocket("/message/{id}") {
        val user = call.parameters["id"]
        call.receiveParameters()
    }
}