package net.unifey.handle

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.*
import io.ktor.server.locations.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import java.time.Duration
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.unifey.Unifey
import net.unifey.auth.isAuthenticated
import net.unifey.handle.admin.adminPages
import net.unifey.handle.beta.betaPages
import net.unifey.handle.communities.routing.communityPages
import net.unifey.handle.emotes.emotePages
import net.unifey.handle.feeds.feedPages
import net.unifey.handle.live.liveSocket
import net.unifey.handle.notification.NotificationManager.postNotification
import net.unifey.handle.reports.reportPages
import net.unifey.handle.users.email.emailPages
import net.unifey.handle.users.userPages
import net.unifey.response.Response
import org.apache.commons.lang3.exception.ExceptionUtils
import org.litote.kmongo.json
import org.slf4j.event.Level

val HTTP_CLIENT =
    HttpClient(CIO) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }

        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

/** the actual server, localhost:8077 :) */
val SERVER =
    embeddedServer(Netty, 8077) {
        install(ContentNegotiation) { json(contentType = ContentType.Application.Json) }

        install(WebSockets) { timeout = Duration.ofSeconds(15) }

        install(Locations)

        install(CallLogging) { level = Level.INFO }

        install(DefaultHeaders) { header("Server", "Unifey v${Unifey.VERSION}") }

        install(AutoHeadResponse)

        install(StatusPages) {
            exception<Error> { call, err -> err.response.invoke(call) }

            /** Error 404 */
            status(HttpStatusCode.NotFound) { call, _ ->
                call.respond(HttpStatusCode.NotFound, Response("That resource was not found."))
            }

            /** Error 401 */
            status(HttpStatusCode.Unauthorized) { call, _ ->
                call.respond(HttpStatusCode.Unauthorized, Response("You are not authorized."))
            }

            exception<Throwable> { call, it ->
                Unifey.ROOT_LOGGER.error("There was an issue.", it)

                Unifey.webhook.sendBigMessage(
                    ExceptionUtils.getStackTrace(it),
                    fileName = "error",
                    message = it.message ?: "No message."
                )

                call.respond(
                    HttpStatusCode.InternalServerError,
                    Response("There was an internal error processing that request.")
                )
            }
        }

        install(CORS) {
            anyHost()

            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
            allowMethod(HttpMethod.Patch)

            allowHeader("Authorization")

            allowNonSimpleContentTypes = true
        }

        routing {
            route("/unifey") {
                @Serializable
                data class VersionEndpointResponse(val endpoint: String, val deprecated: Boolean)

                route("/v1") {
                    emotePages()
                    emailPages()
                    feedPages()
                    userPages()
                    communityPages()
                    reportPages()
                    liveSocket()
                    adminPages()
                    betaPages()

                    get { call.respond(VersionEndpointResponse("/v1", false)) }
                }

                @Serializable
                data class HomeResponse(
                    val payload: String,
                    val uptime: Long,
                    val version: String,
                    val endpoint: String = "/v1",
                    val frontendVersion: String
                )

                get("/") {
                    call.respond(
                        HomeResponse(
                            payload = "unifey! :-)",
                            uptime = System.currentTimeMillis() - Unifey.START_TIME,
                            version = Unifey.VERSION,
                            frontendVersion = Unifey.FRONTEND_EXPECT,
                        )
                    )
                }

                if (!Unifey.prod) {
                    get("/debug-notif") {
                        val token = call.isAuthenticated()

                        token.getOwner().postNotification("Debug notification")

                        call.respond(Response("OK"))
                    }
                }
            }
        }
    }
