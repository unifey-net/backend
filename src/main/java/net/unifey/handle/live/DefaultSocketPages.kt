package net.unifey.handle.live

import net.unifey.Unifey
import net.unifey.handle.live.SocketActionHandler.action
import net.unifey.handle.live.SocketActionHandler.socketActions
import net.unifey.handle.live.objs.ActionHolder
import net.unifey.handle.live.objs.FindActions
import net.unifey.handle.live.objs.SocketType
import net.unifey.handle.users.responses.GetUserResponse.Companion.response

/** The socket types for [defaultSocketPages] */
enum class DefaultSocketPagesTypes : SocketType {
    GET_USER_COUNT,
    GET_SERVER_VERSION,
    GET_USER
}

/** The default socket pages for all websockets. */
@FindActions
object DefaultSocketPages : ActionHolder {
    override val pages: ArrayList<Pair<SocketType, SocketAction>> = socketActions {
        /** Get the amount of users online. */
        action(DefaultSocketPagesTypes.GET_USER_COUNT) {
            respondSuccess(Live.getOnlineUsers().size.toString())
        }

        /** Get the server's version. */
        action(DefaultSocketPagesTypes.GET_SERVER_VERSION) { respondSuccess(Unifey.VERSION) }

        /** Get user. This is used when the frontend starts. */
        action(DefaultSocketPagesTypes.GET_USER) {
            val owner = token.getOwner()

            respondSuccess(owner.response(isSelf = true))
        }
    }
}
