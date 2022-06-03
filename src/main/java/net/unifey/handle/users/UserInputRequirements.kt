package net.unifey.handle.users

import net.unifey.auth.Authenticator
import net.unifey.handle.InvalidVariableInput
import net.unifey.util.InputRequirements

object UserInputRequirements : InputRequirements() {
    private val USERNAME_REGEX = Regex("^[A-Za-z0-9-_]{2,16}\\w+$")
    private val PASSWORD_REGEX =
        Regex(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*[@\$&+,:;=?#|'<>.^*()%!-])[A-Za-z\\d@\$\$&+,:;=?@#|'<>.^*()%!-]{8,128}$"
        )

    /** Check if the username exists as well as doing the checks in [USERNAME] */
    val USERNAME_EXISTS: suspend (String) -> Unit = { username ->
        if (Authenticator.usernameTaken(username))
            throw InvalidVariableInput("username", "That username is already in use!")

        USERNAME.invoke(username)
    }

    /** Check the length and the characters used in a username. Uses [USERNAME_REGEX] */
    val USERNAME: suspend (String) -> Unit = { username ->
        when {
            username.length > 16 ->
                throw InvalidVariableInput(
                    "username",
                    "That username is too long! Maximum is 16 characters."
                )
            3 > username.length ->
                throw InvalidVariableInput(
                    "username",
                    "That username is too short! Minimum is 3 characters."
                )
            !USERNAME_REGEX.matches(username) ->
                throw InvalidVariableInput(
                    "username",
                    "Please only use alphanumerics, - and _ in your username!"
                )
        }
    }

    /** Check if the email already exists as well as doing the checks in [EMAIL] */
    val EMAIL_EXISTS: suspend (String) -> Unit = { email ->
        if (Authenticator.emailInUse(email))
            throw InvalidVariableInput("email", "That email is already in use!")

        EMAIL.invoke(email)
    }

    /** Check if the size is over 128. */
    val EMAIL: suspend (String) -> Unit = { email ->
        when {
            email.length > 128 -> throw InvalidVariableInput("email", "That email is too long!")
        }
    }

    /** Check if the password is 8..128 and check [PASSWORD_REGEX] */
    val PASSWORD: suspend (String) -> Unit = { password ->
        when {
            password.length > 128 ->
                throw InvalidVariableInput(
                    "password",
                    "That password is too long! Maximum is 128 characters."
                )
            8 > password.length ->
                throw InvalidVariableInput(
                    "password",
                    "That password is too short! Minimum is 8 characters."
                )
            !PASSWORD_REGEX.matches(password) ->
                throw InvalidVariableInput(
                    "password",
                    "A password must contain a number, a lowercase letter, uppercase letter and a special character."
                )
        }
    }

    /** Checks if the [username], [password] and [email] are all valid. */
    @Throws(InvalidVariableInput::class)
    suspend fun allMeets(username: String, password: String, email: String) {
        username meets listOf(USERNAME, USERNAME_EXISTS)

        password meets PASSWORD

        password meets listOf(EMAIL, EMAIL_EXISTS)
    }
}
