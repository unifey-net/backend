package net.unifey.handle.communities

import net.unifey.handle.AlreadyExists
import net.unifey.handle.InvalidVariableInput
import net.unifey.util.InputRequirements

object CommunityInputRequirements: InputRequirements() {
    /**
     * To see if names are proper.
     */
    private val NAME_REGEX = Regex("^[A-Za-z0-9-_]{2,16}\\w+$")

    val DESCRIPTION: suspend (String) -> Unit = { desc: String ->
        if (desc.length > 256) {
            throw InvalidVariableInput("Description", "That description is too long! Maximum is 256 characters.")
        }
    }

    val NAME: suspend (String) -> Unit = { name: String ->
        when {
            CommunityManager.nameTakenAsync(name).await() ->
                throw AlreadyExists("community", "name")

            name.length > 16  ->
                throw InvalidVariableInput("Name", "That name is too long! Maximum is 16 characters.")

            3 > name.length ->
                throw InvalidVariableInput("Name", "That name is too short! Minimum is 3 characters.")

            !NAME_REGEX.matches(name) ->
                throw InvalidVariableInput("Name", "Please only use alphanumerics, - and _ in the name!")
        }
    }
}