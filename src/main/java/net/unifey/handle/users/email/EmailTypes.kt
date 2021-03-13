package net.unifey.handle.users.email

import net.unifey.handle.users.email.defaults.Email
import net.unifey.handle.users.email.defaults.PasswordResetEmail
import net.unifey.handle.users.email.defaults.VerifyBeta
import net.unifey.handle.users.email.defaults.VerifyEmail

/**
 * An email [id] to the [Email] class.
 */
enum class EmailTypes(val id: Int, val default: Email) {
    VERIFY_EMAIL(0, VerifyEmail),
    VERIFY_PASSWORD_RESET(1, PasswordResetEmail),
    VERIFY_BETA(2, VerifyBeta)
}