package net.unifey.handle.users.email

/**
 * An email request.
 *
 * An email request can be for a password reset or for verifying that the email exists.
 */
class UserEmailRequest(val id: Long?, val email: String, val verify: String, val type: Int)
