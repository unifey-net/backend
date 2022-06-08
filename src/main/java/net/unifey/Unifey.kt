package net.unifey

import ch.qos.logback.classic.Level.INFO
import ch.qos.logback.classic.Level.OFF
import ch.qos.logback.classic.LoggerContext
import dev.ajkneisl.lib.Lib
import dev.ajkneisl.lib.discord.DiscordWebhook
import io.ktor.server.locations.*
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.unifey.handle.SERVER
import net.unifey.handle.live.SocketActionHandler
import net.unifey.util.SecretsManager
import org.litote.kmongo.id.serialization.IdKotlinXSerializationModule
import org.reflections.Reflections
import org.slf4j.LoggerFactory

object Unifey {
    val ROOT_LOGGER = KotlinLogging.logger {}

    const val VERSION = "0.9.0"
    const val FRONTEND_EXPECT = "0.9.0"

    val START_TIME = System.currentTimeMillis()
    val REFLECTIONS = Reflections("net.unifey")
    val JSON = Json { serializersModule = IdKotlinXSerializationModule }

    lateinit var webhook: DiscordWebhook
    lateinit var mongo: String

    /**
     * If or if not running in production changes a few variables.
     *
     * @see net.unifey.util.URL
     */
    var prod = SecretsManager.getSecret("PROD", "false").toBoolean()

    @KtorExperimentalLocationsAPI
    @JvmStatic
    fun main(args: Array<String>) {
        disableLoggers()
        ROOT_LOGGER.info("BACKEND - $VERSION, $prod")

        mongo = SecretsManager.getSecret("MONGO")
        webhook = DiscordWebhook(SecretsManager.getSecret("WEBHOOK"))

        Lib.DEFAULT_WEBHOOK = webhook

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
