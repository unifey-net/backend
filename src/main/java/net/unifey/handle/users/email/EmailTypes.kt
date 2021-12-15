package net.unifey.handle.users.email

import net.unifey.handle.users.email.defaults.Email

/**
 * An email [id] to the [Email] class.
 */
enum class EmailTypes(val id: Int, val default: Email) {
    VERIFY_EMAIL(0, Email.VERIFY),
    VERIFY_PASSWORD_RESET(1, Email.PASSWORD_RESET)
}