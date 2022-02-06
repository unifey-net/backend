package net.unifey.handle.users.connections.handlers

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket4j
import io.github.bucket4j.local.LocalBucket
import java.time.Duration
import kotlinx.coroutines.delay
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class ConnectionHandler(rateLimit: Bandwidth) {
    private val rateLimitBucket: LocalBucket =
        Bucket4j.builder().addLimit(rateLimit).withMillisecondPrecision().build()

    suspend fun handleRateLimit() {
        val probe = rateLimitBucket.tryConsumeAndReturnRemaining(1)

        LOGGER.trace(
            "Checking rate limit for ${this.javaClass}: ${probe.isConsumed} -> ${probe.nanosToWaitForRefill} (${probe.remainingTokens})"
        )

        if (!probe.isConsumed) {
            delay(Duration.ofNanos(probe.nanosToWaitForRefill).toMillis())
        }
    }

    abstract suspend fun getEmail(token: String): String?

    abstract suspend fun getServiceId(token: String): String?

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
