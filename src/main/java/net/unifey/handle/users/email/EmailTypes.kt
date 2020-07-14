package net.unifey.handle.users.email

import net.unifey.handle.users.email.defaults.Email
import net.unifey.handle.users.email.defaults.PasswordResetEmail
import net.unifey.handle.users.email.defaults.VerifyEmail

enum class EmailTypes(val id: Int, val default: Email) {
    VERIFY_EMAIL(0, VerifyEmail),
    VERIFY_PASSWORD_RESET(1, PasswordResetEmail)
}