package net.unifey.util

import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.request.header
import io.ktor.request.receiveParameters
import io.ktor.request.receiveStream
import io.ktor.response.respond
import net.unifey.handle.BodyTooLarge
import net.unifey.handle.Error
import net.unifey.handle.InvalidArguments
import net.unifey.handle.InvalidType
import net.unifey.prod
import net.unifey.response.Response
import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

/**
 * The API base url. Changes depending on if it's prod or not.
 */
val URL: String
    get() =
        if (prod) {
            "https://api.unifey.net"
        } else {
            "http://localhost:8077"
        }

/**
 * The header for a JPEG image. This is used to make sure an uploaded image is a JPEG.
 */
private val JPEG_HEADER = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())

/**
 * Do multiple checks to ensure the body is a proper image.
 */
suspend fun ApplicationCall.ensureProperImageBody(maxSize: Long = 4_000_000): ByteArray {
    val typeHeader = request.header("Content-Type")

    if (typeHeader != ContentType.Image.JPEG.toString())
        throw InvalidType()

    val bytes = receiveStream().readBytes()

    val header = bytes.take(3).toByteArray()

    if (!header.contentEquals(JPEG_HEADER))
        throw InvalidType()

    try {
        ImageIO.read(ByteArrayInputStream(bytes))
    } catch (ex: Exception) {
        throw InvalidType()
    }

    if (bytes.size > maxSize)
        throw BodyTooLarge()

    return bytes
}

/**
 * Clears invalid HTML tags from [input]
 */
fun cleanInput(input: String): String {
    return Jsoup.clean(input, Whitelist.basic())
}

fun String?.clean(): String? {
    this ?: return null

    return cleanInput(this)
}

/**
 * Check the included reCAPTCHA through body parameters
 */
suspend fun ApplicationCall.checkCaptcha(parameters: Parameters? = null) {
    val params = parameters ?: receiveParameters()
    val captcha = params["captcha"]
            ?: throw InvalidArguments("captcha")

    if (!ReCaptcha.getSuccessAsync(captcha).await())
        throw Error {
            respond(HttpStatusCode.BadRequest, Response("Invalid reCAPTCHA."))
        }
}