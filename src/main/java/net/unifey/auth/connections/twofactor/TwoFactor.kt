package net.unifey.auth.connections.twofactor

import com.warrenstrange.googleauth.GoogleAuthenticator
import mu.KotlinLogging
import net.unifey.handle.AlreadyExists
import net.unifey.handle.NotFound
import net.unifey.handle.mongo.MONGO
import net.unifey.handle.users.UserManager
import org.litote.kmongo.eq

object TwoFactor {
    val logger = KotlinLogging.logger {}
    private data class TwoFactorData(val user: Long, val key: String)

    private val AUTHENTICATOR by lazy { GoogleAuthenticator() }

    /**
     * Find if [user] already has two factor authorization enabled. This means they need to disable
     * it first.
     */
    suspend fun alreadyExists(user: Long): Boolean {
        return MONGO
            .getDatabase("users")
            .getCollection<TwoFactorData>("twofactor")
            .countDocuments(TwoFactorData::user eq user) > 0
    }

    /** Enable two factor authorization. */
    suspend fun enableTwoFactor(user: Long): String {
        val userObj = UserManager.getUser(user)

        if (alreadyExists(userObj.id)) throw AlreadyExists("connections", "2fa")

        val credentials = AUTHENTICATOR.createCredentials()

        val data = TwoFactorData(userObj.id, credentials.key)

        MONGO.getDatabase("users").getCollection<TwoFactorData>("twofactor").insertOne(data)

        logger.debug("2FA: Enabled for $user")

        return data.key
    }

    /** Disable two factor authentication. */
    suspend fun disableTwoFactor(user: Long) {
        MONGO
            .getDatabase("users")
            .getCollection<TwoFactorData>("twofactor")
            .deleteOne(TwoFactorData::user eq user)
    }
}
