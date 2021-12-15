package net.unifey.handle.users.email.defaults

import net.unifey.handle.users.email.UserEmailRequest
import net.unifey.util.FRONTEND_URL
import net.unifey.util.URL

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

    companion object {
        /**
         * Get a [Email] object from file (resources folder)
         */
        fun from(name: String): Email {
            val stream = this::class.java.classLoader.getResourceAsStream(name) ?: throw Exception()

            val lines = stream.reader()
                .readLines()

            return object: Email {
                override fun getSubject(request: UserEmailRequest): String {
                    return lines[0]
                }

                override fun getBody(request: UserEmailRequest): String {
                    return lines
                        .subList(1, lines.size)
                        .joinToString("")
                        .replace("{EMAIL}", request.email)
                        .replace("{VERIFY_CODE}", request.verify)
                        .replace("{BACKEND_URL}", URL)
                        .replace("{FRONTEND_URL}", FRONTEND_URL)
                }
            }
        }

        val PASSWORD_RESET = from("passwordreset.txt")
        val VERIFY = from("verifyemail.txt")
    }
}