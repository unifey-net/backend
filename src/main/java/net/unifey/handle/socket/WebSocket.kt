package net.unifey.handle.socket

import io.ktor.http.cio.websocket.*
import org.json.JSONArray
import org.json.JSONObject

object WebSocket {
    suspend fun WebSocketSession.errorMessage(message: String) =
        outgoing.send(Frame.Text(JSONObject()
            .put("response", message)
            .put("type", "error")
            .toString()))

    suspend fun WebSocketSession.successMessage(message: String) =
        outgoing.send(Frame.Text(JSONObject()
            .put("response", message)
            .put("type", "success")
            .toString()))

    suspend fun WebSocketSession.customTypeMessage(type: String, message: JSONObject) =
        outgoing.send(Frame.Text(JSONObject()
            .put("type", type)
            .put("response", message)
            .toString()))

    suspend fun WebSocketSession.customTypeMessage(type: String, message: JSONArray) =
        outgoing.send(Frame.Text(JSONObject()
            .put("type", type)
            .put("response", message)
            .toString()))

    suspend fun WebSocketSession.authenticateMessage() =
        outgoing.send(Frame.Text(JSONObject()
            .put("response", "welcome :)")
            .put("type", "authenticated")
            .toString()))
}