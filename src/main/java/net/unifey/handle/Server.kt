package net.unifey.handle

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import net.unifey.Unifey
import java.time.Duration
import net.unifey.auth.isAuthenticated
import net.unifey.handle.beta.betaPages
import net.unifey.handle.communities.routing.communityPages
import net.unifey.handle.emotes.emotePages
import net.unifey.handle.feeds.feedPages
import net.unifey.handle.live.liveSocket
import net.unifey.handle.notification.NotificationManager.LOGGER
import net.unifey.handle.notification.NotificationManager.postNotification
import net.unifey.handle.reports.reportPages
import net.unifey.handle.users.email.emailPages
import net.unifey.handle.users.userPages
import net.unifey.response.Response
import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.event.Level
import java.io.File

val HTTP_CLIENT = HttpClient {
    install(JsonFeature) {
        serializer = KotlinxSerializer(kotlinx.serialization.json.Json { ignoreUnknownKeys = true })
        acceptContentTypes = acceptContentTypes + ContentType.Any
    }

    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.INFO
    }
}

/** the actual server, localhost:8077 :) */
val SERVER =
    embeddedServer(Netty, 8077) {
        install(ContentNegotiation) { json(contentType = ContentType.Application.Json) }

        install(io.ktor.websocket.WebSockets) { timeout = Duration.ofSeconds(15) }

        install(Locations)

        install(CallLogging) { level = Level.INFO }

        install(DefaultHeaders) { header("Server", "Unifey v${Unifey.VERSION}") }

        install(AutoHeadResponse)

        install(StatusPages) {
            exception<Error> { it.response.invoke(call) }

            /** Error 404 */
            status(HttpStatusCode.NotFound) {
                call.respond(HttpStatusCode.NotFound, Response("That resource was not found."))
            }

            /** Error 401 */
            status(HttpStatusCode.Unauthorized) {
                call.respond(HttpStatusCode.Unauthorized, Response("You are not authorized."))
            }

            exception<Throwable> {
                Unifey.ROOT_LOGGER.error("There was an issue.", it)

                call.respond(
                    HttpStatusCode.InternalServerError,
                    Response("There was an internal error processing that request.")
                )
            }
        }

        install(CORS) {
            anyHost()

            method(HttpMethod.Options)
            method(HttpMethod.Put)
            method(HttpMethod.Delete)
            method(HttpMethod.Patch)

            header("Authorization")

            allowNonSimpleContentTypes = true
        }

        routing {
            emotePages()
            emailPages()
            feedPages()
            userPages()
            communityPages()
            reportPages()
            liveSocket()

            betaPages()

            get("/") { call.respond(Response("unifey :)")) }

            get("/debug-notif") {
                val token = call.isAuthenticated()

                token.getOwner().postNotification("Debug notification")

                call.respond(Response("OK"))
            }
        }
    }
