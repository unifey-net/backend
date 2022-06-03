package net.unifey.util

import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.server.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import net.unifey.handle.Error
import net.unifey.handle.HTTP_CLIENT

/** Manage Google reCAPTCHA */
object ReCaptcha {
    private val logger = KotlinLogging.logger {}

    /** The secret key to ReCAPTCHA. Required for auth */
    private val SECRET = SecretsManager.getSecret("RECAPTCHA_KEY")

    /**
     * A response to a ReCAPTCHA Request
     *
     * @param success If the ReCAPTCHA request was successful. (What actually matters)
     * @param challengeTs Timestamp of the challenge load (ISO format yyyy-MM-dd'T'HH:mm:ssZZ)
     * @param hostname the hostname of the site where the reCAPTCHA was solved
     * @param errorCodes possible error codes
     */
    @Serializable
    private data class Response(
        val success: Boolean,
        @SerialName("challenge_ts") val challengeTs: String,
        val hostname: String,
        @SerialName("error-codes") val errorCodes: List<String> = listOf()
    )

    /** Get if [response] is successful as async. */
    @Throws(Error::class)
    suspend fun getSuccessAsync(response: String): Boolean {
        val responseObj =
            try {
                HTTP_CLIENT.post("https://www.google.com/recaptcha/api/siteverify") {
                    setBody(FormDataContent(
                        Parameters.build {
                            append("secret", SECRET)
                            append("response", response)
                        }
                    ))
                }.body<Response>()
            } catch (ex: ClientRequestException) {
                return false
            }

        logger.debug(
            "ReCAPTCHA Check: $response -> ${responseObj.success} (${responseObj.challengeTs})"
        )

        return responseObj.success
    }
}
