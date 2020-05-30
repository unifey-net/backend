package net.unifey

import dev.shog.lib.app.cfg.Config
import dev.shog.lib.app.cfg.ConfigHandler
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
import io.ktor.locations.delete
import io.ktor.locations.put
import io.ktor.request.receive
import io.ktor.request.*
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.*
import io.ktor.serialization.DefaultJsonConfiguration
import io.ktor.serialization.serialization
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import net.unifey.auth.Authenticator
import net.unifey.auth.ex.AuthenticationException
import net.unifey.auth.isAuthenticated
import net.unifey.auth.users.*
import net.unifey.feeds.FeedException
import net.unifey.feeds.feedPages
import net.unifey.response.Response
import java.io.File
import java.text.DateFormat

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

        install(DefaultHeaders) {
            header("Server", "Unifey")
        }

        install(AutoHeadResponse)

        install(StatusPages) {
            exception<AuthenticationException> {
                call.respond(HttpStatusCode.Unauthorized, Response(it.message))
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

            get("/") {
                call.respond(Response("Unifey RESTful Backend"))
            }

            put("/friends") {
                val token = call.isAuthenticated()
                val userUID = token.owner

                val params = call.receiveParameters()
                val friendUID = params["uid"]?.toLong()

                if (friendUID != null)
                    FriendManager.addFriend(userUID, friendUID)
                else
                    call.respond(HttpStatusCode.BadRequest, Response("No UID parameter"))
            }

            delete("/friends") {
                val token = call.isAuthenticated()
                val userUID = token.owner

                val params = call.receiveParameters()
                val friendUID = params["uid"]?.toLong()

                if (friendUID != null)
                    FriendManager.removeFriend(userUID, friendUID)
                else
                    call.respond(HttpStatusCode.BadRequest, Response("No UID parameter"))
            }

            get("/friends") {
                val token = call.isAuthenticated()
                val userUID = token.owner

                call.respond(Response(FriendManager.getFriends(userUID) ?: ArrayList<Long>()))
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
                        call.respond(Response(auth))
                    }
                }
            }
        }
    }

    server.start(true)
}