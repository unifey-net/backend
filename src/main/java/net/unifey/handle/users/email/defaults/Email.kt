package net.unifey.handle.users.email.defaults

import net.unifey.handle.users.email.UserEmailRequest

/**
 * An email body.
 */
interface Email {
    /**
     * Create the subject of the email based off a [request].
     */
    fun getSubject(request: UserEmailRequest): String

    /**
     * Create the body of the email based off a [request].
     */
    fun getBody(request: UserEmailRequest): String
}