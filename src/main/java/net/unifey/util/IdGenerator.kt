package net.unifey.util

import java.security.SecureRandom
import kotlin.random.Random
import kotlin.streams.asSequence
import net.unifey.auth.tokens.TokenManager
import org.apache.commons.codec.digest.DigestUtils

object IdGenerator {
    private val SEC_RANDOM = SecureRandom()

    /** Get an 18 long ID */
    suspend fun getSuspensefulId(
        seed: Long = Random.nextLong(),
        assure: (suspend (id: Long) -> Boolean)? = null
    ): Long {
        val r = Random(System.currentTimeMillis() + seed)

        fun generate(): Long {
            return (0 until 14).joinToString("") { r.nextInt(10).toString() }.toLong()
        }

        var id = generate()

        while (assure?.invoke(id) == true) {
            id = generate()
        }

        return id
    }

    /** Get an 18 long ID */
    fun getId(seed: Long = Random.nextLong(), assure: ((id: Long) -> Boolean)? = null): Long {
        val r = Random(System.currentTimeMillis() + seed)

        fun generate(): Long {
            return (0 until 14).joinToString("") { r.nextInt(10).toString() }.toLong()
        }

        var id = generate()

        while (assure?.invoke(id) == true) {
            id = generate()
        }

        return id
    }

    /** Generate a random string [len]. */
    fun generateRandomString(len: Long): String {
        val source = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        return java.util.Random()
            .ints(len, 0, source.length)
            .asSequence()
            .map(source::get)
            .joinToString("")
    }

    /** Generate bytes using [SEC_RANDOM] */
    private fun genBytes(): ByteArray {
        val bytes = ByteArray(128)

        SEC_RANDOM.nextBytes(bytes)

        return bytes
    }

    /** Generate a token */
    suspend fun generateToken(): String {
        val token = DigestUtils.sha256Hex(genBytes())

        if (tokenUsed(token)) return generateToken()

        return token
    }

    /** Check if a token has been used before. */
    private suspend fun tokenUsed(token: String): Boolean {
        return TokenManager.getToken(token) != null
    }
}
