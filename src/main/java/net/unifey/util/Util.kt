package net.unifey.util

import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.request.header
import io.ktor.request.receiveStream
import net.unifey.handle.BodyTooLarge
import net.unifey.handle.InvalidType
import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

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