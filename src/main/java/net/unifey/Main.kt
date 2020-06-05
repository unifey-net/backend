package net.unifey

import dev.shog.lib.app.AppBuilder
import dev.shog.lib.app.cfg.Config
import dev.shog.lib.app.cfg.ConfigHandler
import dev.shog.lib.hook.DiscordWebhook
import dev.shog.lib.util.defaultFormat
import dev.shog.lib.util.fancyDate
import dev.shog.lib.util.logDiscord
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.*
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.JacksonConverter
import io.ktor.jackson.jackson
import io.ktor.locations.Locations
import io.ktor.request.*
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.serialization.serialization
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.future.asDeferred
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import net.unifey.auth.Authenticator
import net.unifey.auth.ex.AuthenticationException
import net.unifey.auth.getTokenFromCall
import net.unifey.handle.AlreadyExists
import net.unifey.handle.ArgumentTooLarge
import net.unifey.handle.InvalidArguments
import net.unifey.handle.NotFound
import net.unifey.handle.communities.communityPages
import net.unifey.handle.users.*
import net.unifey.handle.feeds.feedPages
import net.unifey.response.Response
import net.unifey.util.RateLimitException
import net.unifey.util.checkRateLimit
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

val unifey = AppBuilder("Unifey", 0.3F)
        .usingConfig(ConfigHandler.createConfig(ConfigHandler.ConfigType.YML, "unifey", Config::class.java))
        .configureConfig { cfg ->
            val cfgObj = cfg.asObject<net.unifey.config.Config>()

            this.logger = LoggerFactory.getLogger("Unifey")
            this.webhook = DiscordWebhook(cfgObj.webhook ?: exitProcess(-1))
        }
        .build()

fun main(args: Array<String>) {
    unifey.sendMessage("Unifey backend has started at ${System.currentTimeMillis().defaultFormat()}")

    val server = embeddedServer(Netty, 8080) {
        install(ContentNegotiation) {
            jackson {
            }

            register(ContentType.Application.Json, JacksonConverter())

            serialization(
                    contentType = ContentType.Application.Json,
                    json = Json(JsonConfiguration.Default)
            )
        }

        install(Locations)

        install(CallLogging) {
            level = Level.INFO
        }

        install(DefaultHeaders) {
            header("Server", "Unifey/${unifey.getVersion()}")
        }

        install(AutoHeadResponse)

        install(StatusPages) {
            exception<AuthenticationException> {
                call.respond(HttpStatusCode.Unauthorized, Response(it.message))
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
            header("Authorization")
            allowCredentials = true
            allowNonSimpleContentTypes = true
        }

        routing {
            feedPages()
            userPages()
            friendsPages()
            communityPages()

            get("/") {
                call.checkRateLimit(call.getTokenFromCall())

                call.respond(Response("Unifey RESTful Backend"))
            }

            post("/authenticate") {
                val params = call.receiveParameters();
                val username = params["username"];
                val password = params["password"];

                if (username == null || password == null)
                    call.respond(HttpStatusCode.BadRequest, Response("No username or password parameter."))
                else {
                    val auth = Authenticator.generateIfCorrect(username, password)

                    if (auth == null)
                        call.respond(HttpStatusCode.Unauthorized, Response("Invalid credentials."))
                    else {
                        call.respond(auth)
                    }
                }
            }
        }
    }

    server.start(true)
}