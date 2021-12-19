package net.unifey.handle.live

import io.ktor.http.cio.websocket.*
import org.json.JSONArray
import org.json.JSONObject

object WebSocket {
    suspend fun SocketSession.errorMessage(message: String) =
        session.outgoing.send(
            Frame.Text(JSONObject().put("response", message).put("type", "error").toString()))

    suspend fun SocketSession.successMessage(message: String) =
        session.outgoing.send(
            Frame.Text(JSONObject().put("response", message).put("type", "success").toString()))

    suspend fun SocketSession.customTypeMessage(type: String, message: JSONObject) =
        session.outgoing.send(
            Frame.Text(JSONObject().put("type", type).put("response", message).toString()))

    suspend fun SocketSession.customTypeMessage(type: String, message: String) =
        session.outgoing.send(Frame.Text("{\"type\": \"${type}\", \"response\": $message }"))

    suspend fun WebSocketSession.customTypeMessage(type: String, message: String) =
        outgoing.send(Frame.Text("{\"type\": \"${type}\", \"response\": $message }"))

    suspend fun SocketSession.customTypeMessage(type: String, message: JSONArray) =
        session.outgoing.send(
            Frame.Text(JSONObject().put("type", type).put("response", message).toString()))

    suspend fun SocketSession.authenticateMessage() =
        session.outgoing.send(
            Frame.Text(
                JSONObject().put("response", "welcome :)").put("type", "authenticated").toString()))
    suspend fun WebSocketSession.errorMessage(message: String) =
        outgoing.send(
            Frame.Text(JSONObject().put("response", message).put("type", "error").toString()))

    suspend fun WebSocketSession.successMessage(message: String) =
        outgoing.send(
            Frame.Text(JSONObject().put("response", message).put("type", "success").toString()))

    suspend fun WebSocketSession.customTypeMessage(type: String, message: JSONObject) =
        outgoing.send(
            Frame.Text(JSONObject().put("type", type).put("response", message).toString()))

    suspend fun WebSocketSession.customTypeMessage(type: String, message: JSONArray) =
        outgoing.send(
            Frame.Text(JSONObject().put("type", type).put("response", message).toString()))

    suspend fun WebSocketSession.authenticateMessage() =
        outgoing.send(
            Frame.Text(
                JSONObject().put("response", "welcome :)").put("type", "authenticated").toString()))
}
