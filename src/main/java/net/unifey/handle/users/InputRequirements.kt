package net.unifey.handle.users

import net.unifey.auth.Authenticator
import net.unifey.handle.InvalidVariableInput

object InputRequirements {
    /**
     * To see if emails are proper.
     */
    private val EMAIL_REGEX = Regex("(?:[a-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#\$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])")

    /**
     * To see if usernames are proper.
     */
    val USERNAME_REGEX = Regex("^[A-Za-z0-9-_]{2,16}\\w+$")

    private val PASSWORD_REGEX = Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&-_+=()!]).{8,128}$")

    /**
     * If [email] meets the requirements.
     */
    @Throws(InvalidVariableInput::class)
    fun emailMeets(email: String) {
        when {
            Authenticator.emailInUse(email) ->
               throw InvalidVariableInput("email", "That email is already in use!")

            !EMAIL_REGEX.matches(email) ->
                throw InvalidVariableInput("email", "Please an invalid email!")

            email.length > 128 ->
                throw InvalidVariableInput("email", "That email is too long!")
        }
    }

    /**
     * If [password] meets the requirements.
     */
    @Throws(InvalidVariableInput::class)
    fun passwordMeets(password: String) {
        when {
            password.length > 128  ->
                throw InvalidVariableInput("password", "That password is too long! Maximum is 128 characters.")

            8 > password.length ->
                throw InvalidVariableInput("password", "That password is too short! Minimum is 8 characters.")

            !PASSWORD_REGEX.matches(password) ->
                throw InvalidVariableInput("password", "A password must contain a number, a lowercase letter, uppercase letter and a special character.")
        }
    }

    /**
     * If [username] meets the requirements.
     */
    @Throws(InvalidVariableInput::class)
    fun usernameMeets(username: String) {
        when {
            Authenticator.usernameTaken(username) ->
                throw InvalidVariableInput("username", "That username is already in use!")

            username.length > 16  ->
                throw InvalidVariableInput("username", "That username is too long! Maximum is 16 characters.")

            3 > username.length ->
                throw InvalidVariableInput("username", "That username is too short! Minimum is 3 characters.")

            !USERNAME_REGEX.matches(username) ->
                throw InvalidVariableInput("username", "Please only use alphanumerics, - and _ in your username!")
        }
    }

    /**
     * Checks if the [username], [password] and [email] are all valid.
     */
    @Throws(InvalidVariableInput::class)
    fun allMeets(username: String, password: String, email: String) {
        usernameMeets(username)
        passwordMeets(password)
        emailMeets(email)
    }
}