package net.unifey

import ch.qos.logback.classic.Level.OFF
import ch.qos.logback.classic.LoggerContext
import dev.shog.lib.discord.DiscordWebhook
import dev.shog.lib.discord.WebhookUser
import dev.shog.lib.util.ArgsHandler
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.*
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.websocket.timeout
import io.ktor.jackson.JacksonConverter
import io.ktor.jackson.jackson
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.serialization.serialization
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.WebSockets
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import net.unifey.handle.Error
import net.unifey.handle.SERVER
import net.unifey.handle.beta.betaPages
import net.unifey.handle.communities.CommunityManager
import net.unifey.handle.communities.routing.communityPages
import net.unifey.handle.emotes.emotePages
import net.unifey.handle.feeds.feedPages
import net.unifey.handle.reports.reportPages
import net.unifey.handle.users.UserManager
import net.unifey.handle.users.email.emailPages
import net.unifey.handle.users.friendsPages
import net.unifey.handle.users.userPages
import net.unifey.response.Response
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.time.Duration
import kotlin.reflect.jvm.internal.impl.utils.ExceptionUtilsKt

val logger = LoggerFactory.getLogger("Unifey")

lateinit var webhook: DiscordWebhook
lateinit var mongo: String

/**
 * If or if not running in production changes a few variables.
 *
 * @see net.unifey.util.URL
 */
var prod = System.getenv("PROD")?.toBoolean() ?: false

@UnstableDefault
@KtorExperimentalLocationsAPI
fun main(args: Array<String>) {
    disableLoggers()

    mongo = System.getenv("MONGO")
    webhook = DiscordWebhook(
        System.getenv("WEBHOOK"),
        WebhookUser("Unifey", "https://unifey.net/favicon.png")
    )

    SERVER.start(true)
}

/**
 * Disable the loggers that like to talk a lot :D
 */
fun disableLoggers() {
    val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
    val rootLogger = loggerContext.getLogger("org.mongodb.driver")
    rootLogger.level = OFF

    val awsLogger = loggerContext.getLogger("software.amazon.awssdk")
    awsLogger.level = OFF
}