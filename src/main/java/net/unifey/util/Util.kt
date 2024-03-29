package net.unifey.util

import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import io.ktor.server.request.receiveParameters
import io.ktor.server.request.receiveStream
import io.ktor.server.response.respond
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import net.unifey.Unifey
import net.unifey.handle.BodyTooLarge
import net.unifey.handle.Error
import net.unifey.handle.InvalidArguments
import net.unifey.handle.InvalidType
import net.unifey.handle.users.ShortUser
import net.unifey.handle.users.User
import net.unifey.response.Response
import org.bson.Document
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

/** The API base url. Changes depending on if it's prod or not. */
val URL: String
    get() =
        if (Unifey.prod) {
            "https://api.unifey.app"
        } else {
            "http://localhost:8077"
        }

val FRONTEND_URL: String
    get() =
        if (Unifey.prod) {
            "https://unifey.app"
        } else {
            "http://localhost:3000"
        }

/** The header for a JPEG image. This is used to make sure an uploaded image is a JPEG. */
private val JPEG_HEADER = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())

/** Do multiple checks to ensure the body is a proper image. */
suspend fun ApplicationCall.ensureProperImageBody(maxSize: Long = 4_000_000): ByteArray {
    val typeHeader = request.header("Content-Type")

    if (typeHeader != ContentType.Image.JPEG.toString()) throw InvalidType()

    val bytes = receiveStream().readBytes()

    val header = bytes.take(3).toByteArray()

    if (!header.contentEquals(JPEG_HEADER)) throw InvalidType()

    try {
        ImageIO.read(ByteArrayInputStream(bytes))
    } catch (ex: Exception) {
        throw InvalidType()
    }

    if (bytes.size > maxSize) throw BodyTooLarge()

    return bytes
}

/** Clears invalid HTML tags from [input] */
fun cleanInput(input: String): String {
    return Jsoup.clean(input, Safelist.basic())
}

fun String?.clean(): String? {
    this ?: return null

    return cleanInput(this)
}

fun String.toDocument(): Document {
    return Document.parse(this)
}

/** Check the included reCAPTCHA through body parameters */
suspend fun ApplicationCall.checkCaptcha(parameters: Parameters? = null) {
    val params = parameters ?: receiveParameters()
    val captcha = params["captcha"] ?: throw InvalidArguments("captcha")

    if (!ReCaptcha.getSuccessAsync(captcha))
        throw Error({ respond(HttpStatusCode.BadRequest, Response("Invalid reCAPTCHA.")) })
}

/** Create a mention. Recognizable on the frontend. */
fun User.mention(): String = "@{${id}::${username}}"

/** Create a mention. Recognizable on the frontend. */
fun ShortUser.mention(): String = "@{${id}::${username}}"
