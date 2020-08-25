package net.unifey.handle.users.email

import com.mongodb.client.model.Filters.eq
import io.ktor.application.call
import io.ktor.request.receiveParameters
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.*
import net.unifey.auth.isAuthenticated
import net.unifey.handle.InvalidArguments
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.UserInputRequirements
import net.unifey.handle.users.UserManager
import net.unifey.response.Response

fun Routing.emailPages() {
    route("/email") {
        get {
            val token = call.isAuthenticated()

            call.respond(Response(UserManager.getUser(token.owner).email))
        }

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
         * Get your email status.
         */
        get("/status") {
            val token = call.isAuthenticated()

            val type = call.request.queryParameters["type"]?.toIntOrNull()
                    ?: throw InvalidArguments("type")

            call.respond(Response(UserEmailManager.getRequest(token.owner, type).attempts))
        }

        /**
         * Forgot password.
         */
        put("/forgot") {
            val params = call.receiveParameters()

            val input = params["input"]
                    ?: throw InvalidArguments("input")

            val user = UserPasswordResetHandler.findUsingInput(input)

            UserEmailManager.sendPasswordReset(user)

            call.respond(Response())
        }

        /**
         * Change password using forgot token.
         */
        post("/forgot") {
            val params = call.receiveParameters()

            val verify = params["verify"]
            val password = params["password"]

            if (verify == null || password == null)
                throw InvalidArguments("verify", "password")

            UserInputRequirements.meets(password, UserInputRequirements.PASSWORD)

            UserEmailManager.passwordReset(verify, password)

            call.respond(Response())
        }

        /**
         * Verify email
         */
        post("/verify") {
            val params = call.receiveParameters()
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
         * Beta verify
         */
        post("/betaverify") {
            val params = call.receiveParameters()
            val verify = params["verify"] ?: throw InvalidArguments("verify")

            UserEmailManager.betaVerify(verify)

            call.respond(Response())
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