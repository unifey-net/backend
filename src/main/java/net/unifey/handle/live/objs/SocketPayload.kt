package net.unifey.handle.live.objs

import kotlinx.serialization.Serializable

@Serializable data class SocketPayload(val type: String, val success: Boolean, val payload: String)
