package net.unifey.handle.users.email.defaults

import net.unifey.handle.users.email.UserEmailRequest

/**
 * When a password is requested to be reset.
 */
object PasswordResetEmail : Email {
    override fun getSubject(request: UserEmailRequest): String =
            "Password reset for your Unifey account."

    override fun getBody(request: UserEmailRequest): String =
            buildString {
                append("<h1><img width='32' height='32' src='https://unifey.net/favicon.png'/> Unifey</h1>")
                append("<p>Your Unifey account has requested a password reset. To reset your password, please click on the link below: <br />")
                append("https://unifey.net/settings/forgot?verify=${request.verify}")
                append("<br /><br /> If you didn't request this, please click the link below to remove your email from this account and prevent your email from being used in the future <br />")
                append("https://api.unifey.net/email/unsubscribe?email=${request.email}&verify=${request.verify}")
            }
}