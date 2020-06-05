package net.unifey

import dev.shog.lib.app.cfg.Config
import dev.shog.lib.app.cfg.ConfigHandler
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
import io.netty.handler.logging.LogLevel
import kong.unirest.Unirest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import net.unifey.auth.Authenticator
import net.unifey.auth.ex.AuthenticationException
import net.unifey.auth.getTokenFromCall
import net.unifey.auth.isAuthenticated
import net.unifey.handle.AlreadyExists
import net.unifey.handle.ApiException
import net.unifey.handle.communities.communityPages
import net.unifey.handle.users.*
import net.unifey.handle.feeds.FeedException
import net.unifey.handle.feeds.feedPages
import net.unifey.response.Response
import net.unifey.util.RateLimitException
import net.unifey.util.checkRateLimit
import org.slf4j.event.Level
import java.util.concurrent.TimeUnit

val unifeyCfg = ConfigHandler.createConfig(ConfigHandler.ConfigType.YML, "unifey", Config::class.java)

fun main(args: Array<String>) {
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
            header("Server", "Unifey")
        }

        install(AutoHeadResponse)

        install(StatusPages) {
            exception<AuthenticationException> {
                call.respond(HttpStatusCode.Unauthorized, Response(it.message))
            }

            exception<ApiException> {
                call.respond(HttpStatusCode.BadRequest, Response(it.message))
            }

            exception<RateLimitException> {
                call.response.header(
                        "X-Rate-Limit-Retry-After-Seconds",
                        TimeUnit.NANOSECONDS.toSeconds(it.refill)
                )

                call.respond(HttpStatusCode.TooManyRequests, Response("You are being rate limited!"))
            }

            exception<UserNotFound> {
                call.respond(HttpStatusCode.BadRequest, Response(it.message))
            }

            exception<FeedException> {
                call.respond(HttpStatusCode.BadRequest, Response(it.message))
            }

            exception<Throwable> {
                it.printStackTrace()

                call.respond(HttpStatusCode.InternalServerError, Response("There was an internal error processing that request."))
            }

            status(HttpStatusCode.NotFound) {
                call.respond(HttpStatusCode.NotFound, Response("That resource was not found."))
            }

            status(HttpStatusCode.Unauthorized) {
                call.respond(HttpStatusCode.Unauthorized, Response("You are not authorized."))
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