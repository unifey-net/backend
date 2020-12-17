package net.unifey.util

import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import kong.unirest.Unirest
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.asDeferred
import net.unifey.handle.Error
import net.unifey.handle.InvalidArguments
import net.unifey.response.Response

object ReCaptcha {
    private val SECRET = System.getenv("RECAPTCHA")

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