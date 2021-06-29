package net.unifey.handle

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.jackson.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import net.unifey.handle.beta.betaPages
import net.unifey.handle.communities.routing.communityPages
import net.unifey.handle.emotes.emotePages
import net.unifey.handle.feeds.feedPages
import net.unifey.handle.reports.reportPages
import net.unifey.handle.users.email.emailPages
import net.unifey.handle.users.friendsPages
import net.unifey.handle.users.userPages
import net.unifey.response.Response
import net.unifey.webhook
import org.slf4j.event.Level
import java.time.Duration

/**
 * the actual server, localhost:8077 :)
 */
val SERVER = embeddedServer(Netty, 8077) {
    install(ContentNegotiation) {
        jackson {
        }

        register(ContentType.Application.Json, JacksonConverter())

        serialization(
            contentType = ContentType.Application.Json,
            json = Json(JsonConfiguration.Default)
        )
    }

    install(io.ktor.websocket.WebSockets) {
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