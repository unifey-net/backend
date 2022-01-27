package net.unifey.handle.live.objs

import net.unifey.handle.live.SocketAction

interface ActionHolder {
    val pages: ArrayList<Pair<SocketType, SocketAction>>
}
