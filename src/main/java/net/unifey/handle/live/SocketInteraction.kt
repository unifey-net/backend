package net.unifey.handle.live

/**
 * Annotates a socket interation. This involves receiving and sending requests.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD)
annotation class SocketInteraction(val name: String)