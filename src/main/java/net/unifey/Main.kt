package net.unifey

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type.Text
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.request.receiveParameters
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main(args: Array<String>) {
    val server = embeddedServer(Netty, 8080) {
        routing {
            get("/") {
                call.respondText("no api here retard")
            }
            get("/posts") {
                call.respondText("posts go here")
            }
            post("/authenticate") {
                val params = call.receiveParameters();
                val username = params["username"];
                val password = params["password"];

                if (username == null || password == null)
                    call.respondText("invalid credentials dumb dumb");
                else {
                    println(username)
                    println(password)
                }
            }
        }
    }

    server.start(true)
}