package net.unifey.handle.beta

import net.unifey.handle.InvalidVariableInput
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.UserInputRequirements
import net.unifey.webhook

/**
 * Handles stuff related to beta.
 *
 * This includes bug finding.
 */
object BetaHandler {
    enum class RequestType {
        BUG,
        FEATURE_REQUEST
    }

    data class Request(
        val name: String,
        val authorized: Boolean,
        val type: RequestType,
        val message: String
    )

    /**
     * @param type The type of request.
     * @param message The included message. This includes details about the bug/feature request.
     * @param name The name of the user submitting. This could be their actual name (signified by
     * [authorized])
     * @param authorized If the [name] comes from a real user account
     */
    @Throws(InvalidVariableInput::class)
    suspend fun createRequest(
        type: RequestType,
        message: String,
        name: String,
        authorized: Boolean
    ) {
        UserInputRequirements.meets(name, UserInputRequirements.USERNAME)

        if (message.length !in 1029 downTo -1)
            throw InvalidVariableInput(
                "message", "Message should be between 0 and 1028 characters!")

        Mongo.K_MONGO
            .getDatabase("global")
            .getCollection<Request>("beta_requests")
            .insertOne(Request(name, authorized, type, message))

        webhook.sendMessage("**BETA REQUEST**: [__${type}__, from __${name}__ ($authorized)]\n\n```$message```")
    }
}
