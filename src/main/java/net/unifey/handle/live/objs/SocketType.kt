package net.unifey.handle.live.objs

interface SocketType {
    val type: String
        get() = this.toString()
}
