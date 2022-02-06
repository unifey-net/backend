package net.unifey.util

import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.unifey.handle.Error
import net.unifey.handle.HTTP_CLIENT
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** Manage Google reCAPTCHA */
object ReCaptcha {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    /** The secret key to ReCAPTCHA. Required for auth */
    private val SECRET = System.getenv("RECAPTCHA")

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
                HTTP_CLIENT.post<Response>("https://www.google.com/recaptcha/api/siteverify") {
                    body =
                        FormDataContent(
                            Parameters.build {
                                append("secret", SECRET)
                                append("response", response)
                            }
                        )
                }
            } catch (ex: ClientRequestException) {
                return false
            }

        logger.debug(
            "ReCAPTCHA Check: $response -> ${responseObj.success} (${responseObj.challengeTs})"
        )

        return responseObj.success
    }
}
