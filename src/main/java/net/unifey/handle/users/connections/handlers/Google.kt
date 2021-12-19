package net.unifey.handle.users.connections.handlers

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Refill
import io.ktor.client.features.*
import io.ktor.client.request.*
import java.time.Duration
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.unifey.handle.HTTP_CLIENT

object Google : ConnectionHandler(Bandwidth.classic(10, Refill.greedy(1, Duration.ofSeconds(1)))) {
    @Serializable
    data class UserInfoResponse(
        val id: String,
        val email: String,
        @SerialName("verified_email") val verifiedEmail: Boolean,
        val name: String,
        @SerialName("given_name") val givenName: String,
        val picture: String,
        val locale: String
    )

    override suspend fun getEmail(token: String): String? {
        handleRateLimit()

        LOGGER.trace("Using Google API to find email")

        return try {
            val response =
                HTTP_CLIENT.get<UserInfoResponse>(
                    "https://www.googleapis.com/oauth2/v1/userinfo?alt=json") {
                    header("Authorization", "Bearer $token")
                }

            if (response.verifiedEmail) response.email else null
        } catch (e: ClientRequestException) {
            null
        }
    }

    override suspend fun getServiceId(token: String): String? {
        handleRateLimit()

        LOGGER.trace("Using Google API to find service ID")

        return try {
            HTTP_CLIENT
                .get<UserInfoResponse>("https://www.googleapis.com/oauth2/v1/userinfo?alt=json") {
                    header("Authorization", "Bearer $token")
                }
                .id
        } catch (e: ClientRequestException) {
            null
        }
    }
}
