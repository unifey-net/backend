package net.unifey.response

import kotlinx.serialization.Serializable

/** A plaintext response or errors use this. */
@Serializable data class Response<T : Any?>(val payload: T)
