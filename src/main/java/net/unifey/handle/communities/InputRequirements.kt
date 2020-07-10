package net.unifey.handle.communities

import net.unifey.handle.AlreadyExists
import net.unifey.handle.InvalidVariableInput

object InputRequirements {
    /**
     * To see if names are proper.
     */
    private val NAME_REGEX = Regex("^[A-Za-z0-9-_]{2,16}\\w+$")

    /**
     * If [desc] mmeets the requirements.
     */
    @Throws(InvalidVariableInput::class)
    fun descMeets(desc: String) {
        when {
            desc.length > 256 ->
                throw InvalidVariableInput("description", "That description is too long! Maximum is 256 characters.")
        }
    }

    /**
     * If [name] meets the requirements.
     */
    @Throws(InvalidVariableInput::class)
    fun nameMeets(name: String) {
        when {
            CommunityManager.nameTaken(name) ->
                throw AlreadyExists("community", "name")

            name.length > 16  ->
                throw InvalidVariableInput("name", "That name is too long! Maximum is 16 characters.")

            3 > name.length ->
                throw InvalidVariableInput("name", "That name is too short! Minimum is 3 characters.")

            !NAME_REGEX.matches(name) ->
                throw InvalidVariableInput("name", "Please only use alphanumerics, - and _ in the name!")
        }
    }
}