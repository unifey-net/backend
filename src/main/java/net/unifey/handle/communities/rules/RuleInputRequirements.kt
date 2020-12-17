package net.unifey.handle.communities.rules

import net.unifey.handle.InvalidVariableInput
import net.unifey.util.InputRequirements

object RuleInputRequirements: InputRequirements() {
    val TITLE: suspend (String) -> Unit = { title ->
        if (!(1..64).contains(title.length))
            throw InvalidVariableInput("Title", "The title must be between 1 and 64 characters!")
    }

    val BODY: suspend (String) -> Unit = { body ->
        if (!(1..256).contains(body.length))
            throw InvalidVariableInput("Body", "The title must be between 1 and 256 characters!")
    }
}