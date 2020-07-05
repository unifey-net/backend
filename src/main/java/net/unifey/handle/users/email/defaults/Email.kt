package net.unifey.handle.users.email.defaults

import net.unifey.handle.users.email.UserEmailRequest

interface Email {
    fun getSubject(request: UserEmailRequest): String

    fun getBody(request: UserEmailRequest): String
}