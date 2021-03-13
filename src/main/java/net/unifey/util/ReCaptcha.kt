package net.unifey.util

import kong.unirest.Unirest
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.asDeferred
import net.unifey.handle.Error

/**
 * Manage Google reCAPTCHA
 */
object ReCaptcha {
    private val SECRET = System.getenv("RECAPTCHA")

    /**
     * Get if [response] is successful as async.
     */
    @Throws(Error::class)
    fun getSuccessAsync(response: String): Deferred<Boolean> {
        return Unirest.post("https://www.google.com/recaptcha/api/siteverify")
                .field("secret", SECRET)
                .field("response", response)
                .asJsonAsync()
                .handleAsync { obj, _ -> obj.body.`object`.getBoolean("success")  }
                .asDeferred()
    }
}