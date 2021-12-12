package net.unifey.handle.live

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.shog.lib.util.jsonObjectOf
import dev.shog.lib.util.toJSON
import io.ktor.http.cio.websocket.*
import net.unifey.VERSION
import net.unifey.auth.tokens.Token
import net.unifey.handle.Error
import net.unifey.handle.live.WebSocket.customTypeMessage
import net.unifey.handle.live.WebSocket.errorMessage
import net.unifey.handle.live.WebSocket.successMessage
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
    fun SocketActions.action(name: String, constructor: suspend SocketSession.() -> Boolean = { true }) {
        if (name.isBlank())
            throw Exception("Name cannot be blank!")

        val page = object : SocketAction {
            override suspend fun SocketSession.receive(): Boolean {
                return try {
                    constructor.invoke(this)
                } catch (err: SocketError) {
                    this@receive.session.close(err.reason)
                    false
                } catch (ex: Error) {
                    errorMessage(ex.message)
                    false
                }
            }
        }

        pages.add(name to page)
    }

    init {
        findActions()

        // default pages:
        socketActions {
            /**
             * Get the amount of users online.
             */
            action("GET_USER_COUNT") {
                successMessage("${Live.getOnlineUsers().size}")
                true
            }

            /**
             * Get the server's version.
             */
            action("GET_SERVER_VERSION") {
                successMessage(VERSION)
                true
            }

            /**
             * Get user. This is used when the frontend starts.
             */
            action("GET_USER") {
                val owner = token.getOwner()
                val mapper = jacksonObjectMapper()

                customTypeMessage(
                    "GET_USER",
                    mapper.writeValueAsString(owner)
                )

                true
            }
        }
    }
}