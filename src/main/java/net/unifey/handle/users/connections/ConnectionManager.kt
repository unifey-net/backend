package net.unifey.handle.users.connections

import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.unifey.handle.HTTP_CLIENT
import net.unifey.handle.mongo.Mongo
import org.json.JSONObject
import org.litote.kmongo.eq

object ConnectionManager {
    enum class Type(val onCreate: (JSONObject) -> String) {
        GOOGLE({ json ->
            json.getLong("id").toString()
        })
    }

    data class Connection(val user: Long, val type: Type, val serviceId: String)

    object Google {
        @Serializable
        data class UserInfoResponse(
            val id: String,
            val email: String,
            @SerialName("verified_email")
            val verifiedEmail: Boolean,
            val name: String,
            @SerialName("given_name")
            val givenName: String,
            @SerialName("family_name")
            val familyName: String,
            val picture: String,
            val locale: String
        )

        suspend fun getServiceIdFromAccessToken(token: String): String {
            return HTTP_CLIENT
                .get<UserInfoResponse>("https://www.googleapis.com/oauth2/v1/userinfo?alt=json") {
                    header("Authorization", "Bearer $token")
                }
                .id
        }
    }

    suspend fun findConnection(type: Type, serviceId: String): Connection? {
        return Mongo.K_MONGO
            .getDatabase("users")
            .getCollection<Connection>("connections")
            .find(Connection::serviceId eq serviceId)
            .first()
    }
}