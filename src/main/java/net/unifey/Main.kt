package net.unifey

import ch.qos.logback.classic.Level.OFF
import ch.qos.logback.classic.LoggerContext
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException
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
import io.ktor.locations.Locations
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.serialization.serialization
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.WebSockets
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import net.unifey.auth.ex.AuthenticationException
import net.unifey.handle.*
import net.unifey.handle.communities.communityPages
import net.unifey.handle.emotes.emotePages
import net.unifey.handle.feeds.feedPages
import net.unifey.handle.users.email.emailPages
import net.unifey.handle.users.email.registerEmailExceptions
import net.unifey.handle.users.friendsPages
import net.unifey.handle.users.userPages
import net.unifey.response.Response
import net.unifey.util.RateLimitException
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.time.Duration
import java.util.concurrent.TimeUnit


val unifey = Application("unifey", "0.3.2", ConfigHandler.useConfig(ConfigType.YML, "unifey", net.unifey.config.Config::class.java)) { name, ver, cfg ->
    DiscordWebhook(cfg.asObject<net.unifey.config.Config>().webhook ?: "", WebhookUser("Unifey", "https://unifey.net/favicon.png"))
}

var prod = true

var url = "https://api.unifey.net"

fun main(args: Array<String>) {
    val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
    val rootLogger = loggerContext.getLogger("org.mongodb.driver")
    rootLogger.level = OFF

    val argH = ArgsHandler()

    argH.hook("--dev") {
        prod = false
        url = "http://localhost:8077"
    }

    argH.initWith(args)

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
            header("Server", "Unifey/${unifey.version}")
        }

        install(AutoHeadResponse)

        install(StatusPages) {
            registerEmailExceptions()

            exception<AuthenticationException> {
                call.respond(HttpStatusCode.Unauthorized, Response(it.message))
            }

            exception<NoPermission> {
                call.respond(HttpStatusCode.Unauthorized, Response("You don't have permission for this!"))
            }

            exception<InvalidDefinitionException> {
                call.respond(HttpStatusCode.BadRequest, Response("Invalid body."))
            }

            exception<LimitReached> {
                call.respond(HttpStatusCode.BadRequest, Response("Limit has been reached"))
            }

            exception<NotFound> {
                call.respond(HttpStatusCode.BadRequest, Response(if (it.obj == "")
                    "That could not be found!"
                else {
                    "That ${it.obj} could not be found!"
                }))
            }

            /**
             * The request has invalid arguments or is missing arguments.
             */
            exception<InvalidArguments> {
                call.respond(HttpStatusCode.BadRequest, Response("Required arguments: ${it.args.joinToString(", ")}"))
            }

            /**
             * If an included argument is over the allowed size.
             */
            exception<ArgumentTooLarge> {
                call.respond(HttpStatusCode.BadRequest, Response("${it.arg} must be under ${it.max}."))
            }

            /**
             * If something already exists with the included argument.
             */
            exception<AlreadyExists> {
                call.respond(HttpStatusCode.BadRequest, Response("A ${it.type} with that ${it.arg} already exists!"))
            }

            exception<InvalidVariableInput> {
                call.respond(HttpStatusCode.BadRequest, object {
                    val type = it.type
                    val issue = it.issue
                })
            }

            exception<InvalidType> {
                call.respond(HttpStatusCode.UnsupportedMediaType, Response("Invalid type!"))
            }

            exception<BodyTooLarge> {
                call.respond(HttpStatusCode.PayloadTooLarge, Response("Body is too large!"))
            }

            /**
             * When the user has been rate limited.
             */
            exception<RateLimitException> {
                call.response.header(
                        "X-Rate-Limit-Retry-After-Seconds",
                        TimeUnit.NANOSECONDS.toSeconds(it.refill)
                )

                call.respond(HttpStatusCode.TooManyRequests, Response("You are being rate limited!"))
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
                it.logDiscord(unifey)

                call.respond(HttpStatusCode.InternalServerError, Response("There was an internal error processing that request."))
            }
        }

        install(CORS) {
            anyHost()
            method(HttpMethod.Options)
            method(HttpMethod.Put)
            method(HttpMethod.Delete)
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

            get("/") {
                call.respond(Response("Unifey RESTful Backend"))
            }
        }
    }

    server.start(true)
}