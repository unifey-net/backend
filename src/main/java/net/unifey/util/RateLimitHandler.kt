package net.unifey.util

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Bucket4j
import io.github.bucket4j.Refill
import io.ktor.application.ApplicationCall
import net.unifey.auth.tokens.Token
import java.lang.Exception
import java.security.Security
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

object RateLimitHandler {
    private val buckets = ConcurrentHashMap<String, Bucket>()

    fun getBucket(token: String): Bucket {
        val cacheBucket = buckets[token]

        if (cacheBucket != null)
            return cacheBucket

        val bucket = createBucket()

        buckets[token] = bucket

        return bucket
    }

    private fun createBucket(): Bucket =
            Bucket4j.builder()
                    .addLimit(Bandwidth.classic(
                            10, Refill.greedy(1, Duration.ofSeconds(2))
                    ))
                    .build()
}

fun ApplicationCall.checkRateLimit(token: Token): Long {
    val bucket = RateLimitHandler.getBucket(token.token)

    val probe = bucket.tryConsumeAndReturnRemaining(1)
    if (!probe.isConsumed)
        throw RateLimitException(probe.nanosToWaitForRefill)
    else return probe.remainingTokens
}

class RateLimitException(val refill: Long) : Exception()