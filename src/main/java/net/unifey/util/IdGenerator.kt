package net.unifey.util

import net.unifey.auth.Authenticator
import net.unifey.auth.tokens.TokenManager
import org.apache.commons.codec.digest.DigestUtils
import java.security.SecureRandom
import kotlin.random.Random
import kotlin.streams.asSequence

object IdGenerator {
    /**
     * Get an 18 long ID
     */
    fun getId(seed: Long = Random.nextLong()): Long {
        val r = Random(System.currentTimeMillis() + seed)

        return (0 until 14)
                .joinToString("") { r.nextInt(10).toString() }
                .toLong()
    }

    /**
     * Generate a random string [len].
     */
    fun generateRandomString(len: Long): String {
        val source = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        return java.util.Random().ints(len, 0, source.length)
                .asSequence()
                .map(source::get)
                .joinToString("")
    }

    /**
     * Generate a token
     */
    fun generateToken(): String {
        val random = Random(System.currentTimeMillis() - Random.nextLong())

        val token = DigestUtils.sha256Hex(random.nextBytes(128))

        if (tokenUsed(token))
            return generateToken()
        return token
    }

    /**
     * Check if a token has been used before.
     */
    private fun tokenUsed(token: String): Boolean {
        return TokenManager.getToken(token) != null
    }
}