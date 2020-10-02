package net.unifey

import ch.qos.logback.classic.Level.OFF
import ch.qos.logback.classic.LoggerContext
import dev.shog.lib.app.Application
import dev.shog.lib.app.cfg.ConfigHandler
import dev.shog.lib.app.cfg.ConfigType
import dev.shog.lib.discord.DiscordWebhook
import dev.shog.lib.discord.WebhookUser
import dev.shog.lib.util.ArgsHandler
import dev.shog.lib.util.logDiscord
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
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import net.unifey.handle.Error
import net.unifey.handle.beta.betaPages
import net.unifey.handle.communities.communityPages
import net.unifey.handle.emotes.emotePages
import net.unifey.handle.feeds.feedPages
import net.unifey.handle.reports.reportPages
import net.unifey.handle.users.email.emailPages
import net.unifey.handle.users.friendsPages
import net.unifey.handle.users.userPages
import net.unifey.response.Response
import org.apache.http.util.ExceptionUtils
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.time.Duration
import kotlin.reflect.jvm.internal.impl.utils.ExceptionUtilsKt


lateinit var webhook: DiscordWebhook
lateinit var mongo: String

var prod = true

var url = "https://api.unifey.net"

@UnstableDefault
@KtorExperimentalLocationsAPI
fun main(args: Array<String>) {
    val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
    val rootLogger = loggerContext.getLogger("org.mongodb.driver")
    rootLogger.level = OFF

    val awsLogger = loggerContext.getLogger("software.amazon.awssdk")
    awsLogger.level = OFF

    val argH = ArgsHandler()

    argH.hook("--dev") {
        prod = false
        url = "http://localhost:8077"
    }

    argH.initWith(args)

    mongo = System.getenv("MONGO")
    webhook = DiscordWebhook(System.getenv("WEBHOOK"), WebhookUser("Unifey", "https://unifey.net/favicon.png"))

    val server = embeddedServer(Netty, 8077) {
        install(ContentNegotiation) {
            jackson {
            }

            register(ContentType.Application.Json, JacksonConverter())

            serialization(
                    contentType = ContentType.Application.Json,
                    json = Json(JsonConfiguration.Default)
            )
        }

        install(WebSockets) {
            timeout = Duration.ofSeconds(15)
        }

        install(Locations)

        install(CallLogging) {
            level = Level.INFO
        }

        install(DefaultHeaders) {
            header("Server", "Unifey")
        }

        install(AutoHeadResponse)

        install(StatusPages) {
            exception<Error> {
                it.response.invoke(call)
            }

            /**
             * Error 404
             */
            status(HttpStatusCode.NotFound) {
                call.respond(HttpStatusCode.NotFound, Response("That resource was not found."))
            }

            /**
             * Error 401
             */
            status(HttpStatusCode.Unauthorized) {
                call.respond(HttpStatusCode.Unauthorized, Response("You are not authorized."))
            }

            exception<Throwable> {
                it.printStackTrace()
                webhook.sendBigMessage(it.stackTrace.joinToString("\n"), "Unifey Error: ${it.message}")

                call.respond(HttpStatusCode.InternalServerError, Response("There was an internal error processing that request."))
            }
        }

        install(CORS) {
            anyHost()

            method(HttpMethod.Options)
            method(HttpMethod.Put)
            method(HttpMethod.Delete)
            method(HttpMethod.Patch)

            header("Authorization")
            allowCredentials = true
            allowNonSimpleContentTypes = true
        }

        routing {
            emotePages()
            emailPages()
            feedPages()
            userPages()
            friendsPages()
            communityPages()
            reportPages()

            betaPages()

            get("/") {
                call.respond(Response("unifey :)"))
            }
        }
    }

    server.start(true)
}