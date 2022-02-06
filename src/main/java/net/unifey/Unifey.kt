package net.unifey

import ch.qos.logback.classic.Level.*
import ch.qos.logback.classic.LoggerContext
import dev.ajkneisl.lib.Lib
import dev.ajkneisl.lib.discord.DiscordWebhook
import io.ktor.locations.*
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import net.unifey.handle.SERVER
import net.unifey.handle.communities.CommunityManager
import net.unifey.handle.live.SocketActionHandler
import net.unifey.handle.users.UserManager
import org.litote.kmongo.id.serialization.IdKotlinXSerializationModule
import org.reflections.Reflections
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Unifey {
    /** The version of the backend. */
    const val VERSION = "0.8.0"

    /** Reflections instance */
    val REFLECTIONS = Reflections("net.unifey")

    val JSON = Json {
        serializersModule = IdKotlinXSerializationModule
    }

    /** What version is expected of the frontend. */
    const val FRONTEND_EXPECT = "0.8.0"

    val ROOT_LOGGER: Logger = LoggerFactory.getLogger(this.javaClass)

    lateinit var webhook: DiscordWebhook
    lateinit var mongo: String

    /**
     * If or if not running in production changes a few variables.
     *
     * @see net.unifey.util.URL
     */
    var prod = System.getenv("PROD")?.toBoolean() ?: false

    @KtorExperimentalLocationsAPI
    @JvmStatic
    fun main(args: Array<String>) {
        disableLoggers()
        ROOT_LOGGER.info("BACKEND - $VERSION, $prod")

        mongo = System.getenv("MONGO")
        webhook = DiscordWebhook(System.getenv("WEBHOOK"))

        Lib.DEFAULT_WEBHOOK = webhook

        System.getenv("RECAPTCHA") // to ensure exists.

        SocketActionHandler.findActions()

        SERVER.start(true)
    }

    /** Disable the loggers that like to talk a lot :D */
    private fun disableLoggers() {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        val rootLogger = loggerContext.getLogger("org.mongodb.driver")
        rootLogger.level = INFO

        val awsLogger = loggerContext.getLogger("software.amazon.awssdk")
        awsLogger.level = OFF
    }
}
