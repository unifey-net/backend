package net.unifey.util

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Bucket4j
import io.github.bucket4j.Refill
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.HttpStatusCode
import io.ktor.response.header
import io.ktor.response.respond
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.jvm.Throws
import net.unifey.auth.tokens.Token
import net.unifey.handle.Error
import net.unifey.response.Response
import org.slf4j.LoggerFactory

private val LOGGER = LoggerFactory.getLogger(object {}.javaClass.enclosingClass)

val DEFAULT_PAGE_RATE_LIMIT =
    PageRateLimit(Bandwidth.classic(50, Refill.greedy(1, Duration.ofSeconds(2))))

/** Handle a page's rate limit. */
class PageRateLimit(val bandwidth: Bandwidth) {
    private val buckets = ConcurrentHashMap<String, Bucket>()

    /** Get a [Bucket] by a [token]. */
    fun getBucket(token: String): Bucket {
        val cacheBucket = buckets[token]

        if (cacheBucket != null) return cacheBucket

        val bucket = createBucket()

        buckets[token] = bucket

        return bucket
    }

    /** Create a bucket for a token. */
    private fun createBucket(): Bucket = Bucket4j.builder().addLimit(bandwidth).build()
}

/** Check a user's rate limit using their remoteHost. This isn't very secure :( */
fun ApplicationCall.checkIpRateLimit(pageRateLimit: PageRateLimit): Long {
    val remoteHost = request.origin.remoteHost

    val bucket = pageRateLimit.getBucket(remoteHost)

    LOGGER.trace("RATE LIMIT: $remoteHost -> $bucket")

    val probe = bucket.tryConsumeAndReturnRemaining(1)
    if (!probe.isConsumed) throw RateLimitException(probe.nanosToWaitForRefill)
    else return probe.remainingTokens
}

/** Check a user's rate limit using their [token]. */
@Throws(RateLimitException::class)
fun checkRateLimit(token: Token, pageRateLimit: PageRateLimit): Long {
    val bucket = pageRateLimit.getBucket(token.token)

    val probe = bucket.tryConsumeAndReturnRemaining(1)
    if (!probe.isConsumed) throw RateLimitException(probe.nanosToWaitForRefill)
    else return probe.remainingTokens
}

/** If a user's exceeded their rate limit. */
class RateLimitException(private val refill: Long, private val whenAllowed: Long = -1) :
    Error({
        response.header("X-Rate-Limit-Retry-After-Seconds", TimeUnit.MILLISECONDS.toSeconds(refill))

        if (whenAllowed != -1L) response.header("X-Rate-Limit-Reset", whenAllowed)

        response.header("Access-Control-Expose-Headers", "X-Rate-Limit-Reset")

        respond(HttpStatusCode.TooManyRequests, Response("You are being rate limited!"))
    })
