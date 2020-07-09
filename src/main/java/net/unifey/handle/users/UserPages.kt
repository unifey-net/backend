package net.unifey.handle.users

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.routing.*
import net.unifey.auth.Authenticator
import net.unifey.auth.isAuthenticated
import net.unifey.handle.InvalidArguments
import net.unifey.handle.S3ImageHandler
import net.unifey.util.ensureProperImageBody
import net.unifey.handle.users.responses.AuthenticateResponse
import net.unifey.response.Response

private val JPEG_HEADER = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())

fun Routing.userPages() {
    route("/user") {
        /**
         * Get your own user data.
         */
        get {
            val token = call.isAuthenticated()

            call.respond(Response(UserManager.getUser(token.owner)))
        }

        /**
         * Change our own email
         */
        put("/email") {
            val token = call.isAuthenticated()

            val params = call.receiveParameters()
            val email = params["email"] ?: throw InvalidArguments("email")

            InputRequirements.emailMeets("email")

            UserManager.getUser(token.owner).email = email

            call.respond(HttpStatusCode.OK, Response("Changed email."))
        }

        /**
         * Change your own password.
         */
        put("/password") {
            val token = call.isAuthenticated()

            val params = call.receiveParameters()
            val password = params["password"] ?: throw InvalidArguments("password")

            InputRequirements.passwordMeets(password)

            UserManager.getUser(token.owner).password = password

            call.respond(HttpStatusCode.OK, Response("Password has been updated."))
        }

        /**
         * Change your own name.
         */
        put("/name") {
            val token = call.isAuthenticated()

            val params = call.receiveParameters()
            val username = params["username"] ?: throw InvalidArguments("username")

            InputRequirements.usernameMeets(username)

            UserManager.getUser(token.owner).username = username

            call.respond(HttpStatusCode.OK, Response("Username has been updated."))
        }

        /**
         * Change your own picture;.
         */
        put("/picture") {
            val token = call.isAuthenticated()

            val bytes = call.ensureProperImageBody()

            S3ImageHandler.upload("pfp/${token.owner}.jpg", bytes)

            call.respond(HttpStatusCode.PayloadTooLarge, Response("Image type is not JPEG!"))
        }

        /**
         * Manage other users using usernames.
         */
        route("/name/{name}") {
            /**
             * Get a user's data.
             */
            get {
                val name = call.parameters["name"]

                if (name == null)
                    call.respond(HttpStatusCode.BadRequest, Response("No name parameter"))
                else
                    call.respond(Response(UserManager.getId(name)))
            }

            /**
             * Get a user's picture.
             */
            get("/picture") {
                val name = call.parameters["name"]
                        ?: throw InvalidArguments("name")

                call.respondBytes(S3ImageHandler.getPicture("pfp/${UserManager.getId(name)}.jpg", "pfp/default.jpg"), ContentType.Image.JPEG)
            }
        }

        /**
         * Manage other users using IDs.
         */
        route("/id/{id}") {
            get {
                val id = call.parameters["id"]?.toLongOrNull()
                        ?: throw InvalidArguments("id")

                call.respond(UserManager.getUser(id))
            }

            get("/picture") {
                val id = call.parameters["id"]?.toLongOrNull()
                        ?: throw InvalidArguments("id")

                call.respondBytes(S3ImageHandler.getPicture("pfp/${UserManager.getUser(id).id}.jpg", "pfp/default.jpg"), ContentType.Image.JPEG)
            }
        }

        /**
         * Register an account
         */
        put("/register") {
            val params = call.receiveParameters()

            val username = params["username"]
            val password = params["password"]
            val email = params["email"]

            if (username == null || password == null || email == null)
                throw InvalidArguments("username", "password", "email")

            call.respond(UserManager.createUser(email, username, password))
        }
    }

    /**
     * Authenticate. Input a username and password in return for a token
     */
    post("/authenticate") {
        val params = call.receiveParameters()

        val username = params["username"]
        val password = params["password"]

        if (username == null || password == null)
            call.respond(HttpStatusCode.BadRequest, Response("No username or password parameter."))
        else {
            val auth = Authenticator.generateIfCorrect(username, password)

            if (auth == null)
                call.respond(HttpStatusCode.Unauthorized, Response("Invalid credentials."))
            else {
                call.respond(AuthenticateResponse(auth, UserManager.getUser(auth.owner)))
            }
        }
    }
}