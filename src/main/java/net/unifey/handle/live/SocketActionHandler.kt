package net.unifey.handle.live

import dev.shog.lib.util.jsonObjectOf
import dev.shog.lib.util.toJSON
import io.ktor.http.cio.websocket.*
import net.unifey.VERSION
import net.unifey.auth.tokens.Token
import net.unifey.handle.socket.WebSocket.customTypeMessage
import net.unifey.handle.socket.WebSocket.successMessage
import net.unifey.handle.users.User
import net.unifey.handle.users.UserManager
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
            /**
             * Get the amount of users online.
             */
            action("GET_USER_COUNT") { _, _ ->
                successMessage("${Live.getOnlineUsers().size}")
                true
            }

            /**
             * Get the server's version.
             */
            action("GET_SERVER_VERSION") { _, _ ->
                successMessage(VERSION)
                true
            }

            /**
             * Get user. This is used when the frontend starts.
             */
            action("GET_USER") { token, _ ->
                val owner = token.getOwner()

                customTypeMessage(
                    "GET_USER",
                    jsonObjectOf(
                        "id" to owner.id,
                        "username" to owner.username,
                        "role" to owner.role,
                        "verified" to owner.verified,
                        "createdAt" to owner.createdAt,
                        "member" to jsonObjectOf(
                            "id" to owner.member.id,
                            "notifications" to owner.member.getNotifications().toJSON(),
                            "member" to owner.member.getMembers().toJSON()
                        ),
                        "profile" to jsonObjectOf(
                            "id" to owner.profile.id,
                            "cosmetics" to owner.profile.cosmetics.map { cosmetic ->
                                jsonObjectOf(
                                    "id" to cosmetic.id,
                                    "type" to cosmetic.type,
                                    "desc" to cosmetic.desc
                                )
                            },
                            "description" to owner.profile.description,
                            "discord" to owner.profile.discord,
                            "location" to owner.profile.location
                        )
                    )
                )

                true
            }
        }
    }
}