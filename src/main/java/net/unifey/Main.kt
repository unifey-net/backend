package net.unifey

import ch.qos.logback.classic.Level.OFF
import ch.qos.logback.classic.LoggerContext
import com.sendgrid.*
import dev.shog.lib.discord.DiscordWebhook
import dev.shog.lib.discord.WebhookUser
import io.ktor.locations.*
import net.unifey.handle.SERVER
import net.unifey.handle.messaging.messageSocketActions
import net.unifey.handle.notification.notificationSocketActions
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** The version of the backend. */
const val VERSION = "0.8.0"

/** What version is expected of the frontend. */
const val FRONTEND_EXPECT = "0.8.0"

val ROOT_LOGGER: Logger = LoggerFactory.getLogger(object {}.javaClass.enclosingClass)

lateinit var webhook: DiscordWebhook
lateinit var mongo: String

/**
 * If or if not running in production changes a few variables.
 *
 * @see net.unifey.util.URL
 */
var prod = System.getenv("PROD")?.toBoolean() ?: false

@KtorExperimentalLocationsAPI
fun main(args: Array<String>) {
    disableLoggers()

    ROOT_LOGGER.info("BACKEND - $VERSION")

    notificationSocketActions()
    messageSocketActions()

    mongo = System.getenv("MONGO")
    webhook =
        DiscordWebhook(
            System.getenv("WEBHOOK"), WebhookUser("Unifey", "https://unifey.net/favicon.png"))
    System.getenv("RECAPTCHA") // to ensure exists.

    SERVER.start(true)
}

/** Disable the loggers that like to talk a lot :D */
fun disableLoggers() {
    val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
    val rootLogger = loggerContext.getLogger("org.mongodb.driver")
    rootLogger.level = OFF

    val awsLogger = loggerContext.getLogger("software.amazon.awssdk")
    awsLogger.level = OFF
}
