package net.unifey.handle.live

import io.ktor.websocket.*
import net.unifey.Unifey
import net.unifey.handle.Error
import net.unifey.handle.live.objs.ActionHolder
import net.unifey.handle.live.objs.FindActions
import net.unifey.handle.live.objs.SocketSession
import net.unifey.handle.live.objs.SocketType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object SocketActionHandler {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /** All socket actions. */
    val socketActions = arrayListOf<Pair<SocketType, SocketAction>>()

    /** Find pages using [SocketInteraction]. */
    fun findActions() {
        logger.trace("Looking for actions in net.unifey...")
        val types = Unifey.REFLECTIONS.getTypesAnnotatedWith(FindActions::class.java)

        types.forEach { type ->
            val pages = ((type.fields[0].get(type)) as ActionHolder).pages

            logger.trace("Found ${type.simpleName}, ${pages.size} actions.")

            socketActions.addAll(pages)
        }
    }

    /** Holds pages for [socketActions]. */
    class SocketActions {
        val pages: ArrayList<Pair<SocketType, SocketAction>> = arrayListOf()
    }

    /** Constructor that holds [action] */
    fun socketActions(pages: SocketActions.() -> Unit): ArrayList<Pair<SocketType, SocketAction>> {
        val pagesInst = SocketActions()

        pages.invoke(pagesInst)

        return pagesInst.pages
    }

    /** Create a action with [name]. */
    fun SocketActions.action(type: SocketType, constructor: suspend SocketSession.() -> Unit) {
        if (type.type.isBlank()) throw Exception("Name cannot be blank!")

        val page =
            object : SocketAction {
                override suspend fun SocketSession.receive() {
                    return try {
                        constructor.invoke(this)
                    } catch (err: SocketError) {
                        this@receive.session.close(err.reason)
                    } catch (ex: Error) {
                        respond(type, ex)
                    }
                }
            }

        pages.add(type to page)
    }
}
