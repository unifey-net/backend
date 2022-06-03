package net.unifey.util

import mu.KotlinLogging
import net.unifey.handle.NotFound
import org.json.JSONObject

/** Manages secrets passed through environment. */
object SecretsManager {
    private val LOGGER = KotlinLogging.logger {}

    /** Secrets */
    private val secrets by lazy {
        try {
            JSONObject(System.getenv("SECRETS"))
        } catch (ex: Exception) {
            LOGGER.error("FATAL: There was an issue loading secrets.")
            JSONObject()
        }
    }

    /**
     * Get a secret by it's [key].
     *
     * If [key] isn't found, [fallback] will be returned. if [fallback] is null, [NotFound] will be
     * thrown.
     */
    @Throws(NotFound::class)
    fun getSecret(key: String, fallback: String? = null): String {
        if (secrets.has(key)) return secrets.getString(key)
        else if (fallback != null) return fallback

        throw NotFound("Issue finding secret: $key")
    }
}
