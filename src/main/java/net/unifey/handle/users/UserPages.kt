package net.unifey.handle.users

import io.ktor.application.ApplicationCall
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
import net.unifey.handle.live.SocketInteraction
import net.unifey.handle.users.email.Unverified
import net.unifey.handle.users.email.UserEmailManager
import net.unifey.handle.users.friends.friendsPages
import net.unifey.handle.users.profile.cosmetics.cosmeticPages
import net.unifey.handle.users.profile.profilePages
import net.unifey.util.ensureProperImageBody
import net.unifey.handle.users.responses.AuthenticateResponse
import net.unifey.prod
import net.unifey.response.Response
import net.unifey.util.checkCaptcha
import org.mindrot.jbcrypt.BCrypt

fun Routing.userPages() {
    route("/user") {
        route("/cosmetic", cosmeticPages())
        route("/friends", friendsPages())
        route("/profile", profilePages())

        /**
         * Get your own user data.
         */
        get {
            val token = call.isAuthenticated()

            call.respond(Response(UserManager.getUser(token.owner)))
        }

        /**
         * Change a user using [param]
         */
        @Throws(Unverified::class, InvalidArguments::class)
        suspend fun ApplicationCall.changeUser(param: String): Pair<User, String> {
            val token = isAuthenticated()
            val user = UserManager.getUser(token.owner)

            if (!user.verified)
                throw Unverified()

            val params = receiveParameters()
            val par = params[param] ?: throw InvalidArguments(param)

            return user to par
        }

        /**
         * Change your own email
         */
        put("/email") {
            val (user, email) = call.changeUser("email")

            UserInputRequirements.meets(email, UserInputRequirements.EMAIL_EXISTS)

            user.email = email
            user.verified = false

            UserEmailManager.sendVerify(user.id, email)

            call.respond(HttpStatusCode.OK, Response("Changed email."))
        }

        /**
         * Change your own password.
         */
        put("/password") {
            val (user, password) = call.changeUser("password")

            UserInputRequirements.meets(password, UserInputRequirements.PASSWORD)

            user.password = BCrypt.hashpw(password, BCrypt.gensalt())

            call.respond(HttpStatusCode.OK, Response("Password has been updated."))
        }

        /**
         * Change your own name.
         */
        put("/name") {
            val (user, username) = call.changeUser("username")

            UserInputRequirements.meets(username, UserInputRequirements.USERNAME_EXISTS)

            user.username = username

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

            // only check for captcha in production
            if (prod)
                call.checkCaptcha(params)

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

        // only check for captcha in production
        if (prod)
            call.checkCaptcha(params)

        val username = params["username"]
        val password = params["password"]
        val remember = params["remember"]?.toBoolean()

        if (username == null || password == null || remember == null)
            throw InvalidArguments("username", "password", "remember")

        val auth = Authenticator.generateIfCorrect(username, password, remember)

        call.respond(AuthenticateResponse(auth, UserManager.getUser(auth.owner)))
    }
}