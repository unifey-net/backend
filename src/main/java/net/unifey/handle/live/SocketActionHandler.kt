package net.unifey.handle.live

import io.ktor.http.cio.websocket.*
import net.unifey.VERSION
import net.unifey.auth.tokens.Token
import net.unifey.handle.socket.WebSocket.successMessage
import org.json.JSONObject
import org.reflections.Reflections
import java.lang.Exception

object SocketActionHandler {
    /**
     * All socket actions.
     */
    val socketActions = arrayListOf<Pair<String, SocketAction>>()

    /**
     * Find pages using [SocketInteraction].
     */
    private fun findActions() {
        val types = Reflections("net.unifey")
            .getTypesAnnotatedWith(SocketInteraction::class.java)

        val found = arrayListOf<Pair<String, SocketAction>>()

        types.forEach { type ->
            val instance = type.getDeclaredConstructor().newInstance()

            if (instance is SocketAction) {
                val anno = type.annotations.single { ann -> ann is SocketInteraction } as SocketInteraction

                found.add(anno.name to instance)
            }
        }

        socketActions.addAll(found)
    }

    /**
     * Holds pages for [socketActions].
     */
    class SocketActions {
        val pages: ArrayList<Pair<String, SocketAction>> = arrayListOf()
    }

    /**
     * Constructor that holds [action]
     */
    fun socketActions(pages: SocketActions.() -> Unit) {
        val pagesInst = SocketActions()

        pages.invoke(pagesInst)

        this.socketActions.addAll(pagesInst.pages)
    }

    /**
     * Create a action with [name].
     */
    fun SocketActions.action(name: String, constructor: suspend WebSocketSession.(token: Token, data: JSONObject) -> Boolean = { _, _ -> true }) {
        if (name.isBlank())
            throw Exception("Name cannot be blank!")

        val page = object : SocketAction {
            override suspend fun WebSocketSession.receive(auth: Token, data: JSONObject): Boolean {
                return constructor.invoke(this, auth, data)
            }
        }

        pages.add(name to page)
    }

    init {
        findActions()

        // default pages:
        socketActions {
            action("GET_USER_COUNT") { _, _ ->
                successMessage("${Live.getOnlineUsers().size}")
                true
            }

            action("GET_SERVER_VERSION") { _, _ ->
                successMessage(VERSION)
                true
            }
        }
    }
}