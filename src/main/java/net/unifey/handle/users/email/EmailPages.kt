package net.unifey.handle.users.email

import com.mongodb.client.model.Filters.eq
import io.ktor.application.call
import io.ktor.request.receiveParameters
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import net.unifey.handle.InvalidArguments
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.UserManager
import net.unifey.response.Response

fun Routing.emailPages() {
    route("/email") {
        /**
         * Resend an email
         */
        post("/resend") {
            val params = call.receiveParameters()
            val id = params["id"]?.toLongOrNull()
            val type = params["type"]?.toIntOrNull()

            if (id == null || type == null)
                throw InvalidArguments("id", "type")

            UserEmailManager.resendEmail(id, type)

            call.respond(Response())
        }

        /**
         * Verify email
         */
        get("/verify") {
            val params = call.request.queryParameters
            val id = params["id"]?.toLongOrNull()
            val verify = params["verify"]

            if (id == null || verify == null)
                throw InvalidArguments("id", "verify")

            UserEmailManager.verify(id, verify)

            call.respond(Response())
        }

        /**
         * Unsubscribe from Unifey and disallow an email from being able to be used again.
         */
        get("/unsubscribe") {
            val params = call.request.queryParameters
            val email = params["email"]
            val verify = params["verify"]

            if (email == null || verify == null)
                throw InvalidArguments("email", "verify")

            val doc = Mongo.getClient()
                    .getDatabase("email")
                    .getCollection("verify")
                    .find(eq("verify", verify))
                    .firstOrNull()

            if (doc != null) {
                val id = doc.getLong("id")
                val user = UserManager.getUser(id)

                if (user.email == email) {
                    user.verified = false
                    user.email = ""

                    Mongo.getClient()
                            .getDatabase("email")
                            .getCollection("verify")
                            .deleteOne(eq("id", id))

                    UserEmailManager.unSubscribe(email)

                    call.respondRedirect("https://unifey.net/unsubscribed", true)
                    return@get
                }
            }

            throw InvalidArguments("verify", "email")
        }

        /**
         * When a bounce is received from AWS.
         */
        post("/bounce") {
            UserEmailManager.handleBounce(call.receiveText())

            call.respond(Response())
        }
    }
}