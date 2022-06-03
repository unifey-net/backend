package net.unifey.util

import net.unifey.handle.InvalidVariableInput

/**
 * Requirements for input provided by users.
 *
 * Something such as a username. It must meet a certain regex, size and must not be taken.
 */
abstract class InputRequirements {
    /**
     * If [string] meets the length requirements of [input].
     *
     * If it doesn't this will throw an exception. This is to provide the user with more information
     * on how they can fix the issue.
     */
    @Throws(InvalidVariableInput::class)
    suspend fun meets(string: String, input: suspend (String) -> Unit) = input.invoke(string)

    /** Check multiple [checks] using [meets] */
    @Throws(InvalidVariableInput::class)
    suspend fun meets(checks: List<Pair<String, suspend (String) -> Unit>>) =
        checks.forEach { (input, check) -> meets(input, check) }

    companion object {
        /**
         * Infix implementation of the [meets] function.
         */
        suspend infix fun String.meets(input: suspend (String) -> Unit) =
            input.invoke(this)

        /**
         * Infix implementation of [meets] function with multiple different checks.
         */
        suspend infix fun String.meets(inputs: List<suspend (String) -> Unit>) {
            inputs.forEach {
                it.invoke(this@meets)
            }
        }
    }
}
