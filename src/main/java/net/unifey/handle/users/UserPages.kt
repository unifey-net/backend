package net.unifey.handle.users

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import java.util.concurrent.TimeUnit
import net.unifey.Unifey
import net.unifey.auth.AuthenticationException
import net.unifey.auth.Authenticator
import net.unifey.auth.isAuthenticated
import net.unifey.handle.AlreadyExists
import net.unifey.handle.InvalidArguments
import net.unifey.handle.S3ImageHandler
import net.unifey.handle.notification.NotificationManager
import net.unifey.handle.users.connections.ConnectionManager
import net.unifey.handle.users.connections.connectionPages
import net.unifey.handle.users.connections.handlers.Google
import net.unifey.handle.users.email.Unverified
import net.unifey.handle.users.email.UserEmailManager
import net.unifey.handle.users.friends.friendsPages
import net.unifey.handle.users.profile.cosmetics.cosmeticPages
import net.unifey.handle.users.profile.profilePages
import net.unifey.handle.users.responses.AuthenticateResponse
import net.unifey.handle.users.responses.GetUserResponse.Companion.response
import net.unifey.response.Response
import net.unifey.util.FieldChangeLimiter
import net.unifey.util.RateLimitException
import net.unifey.util.checkCaptcha
import net.unifey.util.ensureProperImageBody
import org.mindrot.jbcrypt.BCrypt

fun Routing.userPages() {
    route("/user") {
        route("/cosmetic", cosmeticPages())
        route("/friends", friendsPages())
        route("/profile", profilePages())
        route("/connections", connectionPages())

        /** Get your own user data. */
        get {
            val token = call.isAuthenticated()

            call.respond(token.getOwner().response())
        }

        /** Change a user using [param] */
        @Throws(Unverified::class, InvalidArguments::class)
        suspend fun ApplicationCall.changeUser(param: String): Pair<User, String> {
            val token = isAuthenticated()
            val user = UserManager.getUser(token.owner)

            if (!user.verified) throw Unverified()

            val params = receiveParameters()
            val par = params[param] ?: throw InvalidArguments(param)

            return user to par
        }

        /** Change your own email */
        put("/email") {
            val (user, email) = call.changeUser("email")

            UserInputRequirements.meets(email, UserInputRequirements.EMAIL_EXISTS)

            UserManager.changeEmail(user.id, email)
            UserManager.setVerified(user.id, false)

            UserEmailManager.sendVerify(user.id, email)

            call.respond(Response("Changed email."))
        }

        /** Change your own password. */
        put("/password") {
            val (user, password) = call.changeUser("password")

            UserInputRequirements.meets(password, UserInputRequirements.PASSWORD)

            UserManager.changePassword(user.id, BCrypt.hashpw(password, BCrypt.gensalt()))

            UserManager.signOutAll(user)

            NotificationManager.postNotification(
                user.id,
                "Your password has successfully been changed!"
            )

            call.respond(Response("Password has been updated."))
        }

        /** Change your own name. */
        put("/name") {
            val (user, username) = call.changeUser("username")

            UserInputRequirements.meets(username, UserInputRequirements.USERNAME_EXISTS)

            val (limited, until) = FieldChangeLimiter.isLimited("USER", user.id, "USERNAME")

            if (limited && until != null)
                throw RateLimitException(until - System.currentTimeMillis(), until)

            UserManager.changeUsername(user.id, username)

            FieldChangeLimiter.createLimit(
                "USER",
                user.id,
                "USERNAME",
                System.currentTimeMillis() + TimeUnit.DAYS.toMillis(31)
            )

            call.respond(Response("Username has been updated."))
        }

        /** Change your own picture;. */
        put("/picture") {
            val token = call.isAuthenticated()

            val bytes = call.ensureProperImageBody()

            S3ImageHandler.upload("pfp/${token.owner}.jpg", bytes)

            call.respond(HttpStatusCode.PayloadTooLarge, Response("Image type is not JPEG!"))
        }

        /** Manage other users using usernames. */
        route("/name/{name}") {
            /** Get a user's data. */
            get {
                val name = call.parameters["name"]

                if (name == null)
                    call.respond(HttpStatusCode.BadRequest, Response("No name parameter"))
                else call.respond(Response(UserManager.getId(name)))
            }

            /** Get a user's picture. */
            get("/picture") {
                val name = call.parameters["name"] ?: throw InvalidArguments("name")

                call.respondBytes(
                    S3ImageHandler.getPicture(
                        "pfp/${UserManager.getId(name)}.jpg",
                        "pfp/default.jpg"
                    ),
                    ContentType.Image.JPEG
                )
            }
        }

        /** Manage other users using IDs. */
        route("/id/{id}") {
            get {
                val id = call.parameters["id"]?.toLongOrNull() ?: throw InvalidArguments("id")

                call.respond(UserManager.getUser(id).response())
            }

            get("/picture") {
                val id = call.parameters["id"]?.toLongOrNull() ?: throw InvalidArguments("id")

                call.respondBytes(
                    S3ImageHandler.getPicture(
                        "pfp/${UserManager.getUser(id).id}.jpg",
                        "pfp/default.jpg"
                    ),
                    ContentType.Image.JPEG
                )
            }
        }

        @Throws(InvalidArguments::class, AlreadyExists::class)
        suspend fun useAutoConnect(
            params: Parameters
        ): Triple<ConnectionManager.Type, String, String>? {
            val autoConType =
                try {
                    ConnectionManager.Type.valueOf(params["autoConType"] ?: "")
                } catch (ex: Exception) {
                    null
                }
            val autoConToken = params["autoConToken"]

            if (autoConType != null && autoConToken != null) {
                val serviceId =
                    autoConType.handler.getServiceId(autoConToken)
                        ?: throw InvalidArguments("autoConToken")

                val connection = ConnectionManager.findConnection(autoConType, serviceId)

                if (connection != null) throw AlreadyExists("connection", "token")
                else {
                    val email =
                        autoConType.handler.getEmail(autoConToken)
                            ?: throw InvalidArguments("autoConToken")

                    return Triple(autoConType, serviceId, email)
                }
            }

            return null
        }

        /** Register an account */
        put("/register") {
            val params = call.receiveParameters()

            // only check for captcha in production
            if (Unifey.prod) call.checkCaptcha(params)

            val username = params["username"]
            val password = params["password"]
            val email = params["email"]

            if (username == null || password == null || email == null)
                throw InvalidArguments("username", "password", "email")

            val autoCon = useAutoConnect(params)
            val verified =
                autoCon != null &&
                    autoCon.third.equals(
                        email,
                        true
                    ) // if they're using a connection & the inputted is same as service,
            // auto verify

            val user = UserManager.createUser(email, username, password, verified = verified)

            if (autoCon != null) {
                val (type, id) = autoCon

                ConnectionManager.createConnection(type, user.id, id)
            }

            call.respond(Authenticator.generate(user.id))
        }
    }

    /** Authenticate. Input a username and password in return for a token */
    route("/authenticate") {
        route("/connections") {
            post("/google") {
                val params = call.receiveParameters()

                val accessToken = params["token"] ?: throw InvalidArguments("token")

                val connection =
                    ConnectionManager.findConnection(
                        ConnectionManager.Type.GOOGLE,
                        Google.getServiceId(accessToken) ?: throw InvalidArguments("token")
                    )

                if (connection != null)
                    call.respond(
                        AuthenticateResponse(
                            Authenticator.generate(connection.user),
                            UserManager.getUser(connection.user)
                        )
                    )
                else throw AuthenticationException("Invalid access token")
            }
        }

        post {
            val params = call.receiveParameters()

            // only check for captcha in production
            if (Unifey.prod) call.checkCaptcha(params)

            val username = params["username"]
            val password = params["password"]
            val remember = params["remember"]?.toBoolean()

            if (username == null || password == null || remember == null)
                throw InvalidArguments("username", "password", "remember")

            try {
                UserManager.getUser(username).email.endsWith("@unifey.app")
                println(call.request.headers.toMap().toList().joinToString { (key, value) -> "${key}: $value" })
            } catch (ex: Error) {
            }

            val auth = Authenticator.generateIfCorrect(username, password, remember)

            call.respond(AuthenticateResponse(auth, UserManager.getUser(auth.owner)))
        }
    }
}
