package net.unifey.handle.users

import com.sun.mail.smtp.SMTPTransport
import dev.shog.lib.app.cfg.ConfigHandler
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.*
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.routing.*
import net.unifey.DatabaseHandler
import net.unifey.auth.Authenticator
import net.unifey.auth.isAuthenticated
import net.unifey.config.Config
import net.unifey.handle.InvalidArguments
import net.unifey.handle.users.responses.AuthenticateResponse
import net.unifey.response.Response
import net.unifey.unifey
import net.unifey.util.IdGenerator
import java.lang.Exception
import java.util.*
import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

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
            val email = params["email"]

            if (email == null)
                call.respond(HttpStatusCode.BadRequest, Response("No email parameter"))
            else {
                UserManager.getUser(token.owner).email = email

                when {
                    email.length > 120 ->
                        call.respond(HttpStatusCode.BadRequest, Response("Email is too long! (must be <=120)"))

                    !UserManager.EMAIL_REGEX.matches(email) ->
                        call.respond(HttpStatusCode.BadRequest, Response("Not a proper email!"))

                    else -> {

                        call.respond(HttpStatusCode.OK, Response("Changed email."))
                    }
                }
            }
        }

        /**
         * Verify emails
         */
        get("/verify") {
            val params = call.request.queryParameters
            val id = params["id"]?.toLongOrNull()
            val verify = params["verify"]

            if (id == null || verify == null)
                throw InvalidArguments("id", "verify")

            val rs = DatabaseHandler.getConnection()
                    .prepareStatement("SELECT * FROM verify WHERE id = ? AND verify = ?")
                    .apply {
                        setLong(1, id)
                        setString(2, verify)
                    }
                    .executeQuery()

            if (rs.next()) {
                UserManager.getUser(id).verified = true

                DatabaseHandler.getConnection()
                        .prepareStatement("DELETE FROM verify WHERE id = ?")
                        .apply { setLong(1, id) }
                        .executeUpdate()

                call.respond(HttpStatusCode.OK, Response("Email verified"))
            } else throw InvalidArguments("verify", "id")
        }

        /**
         * Get instead of post to allow for users to do it through browser.
         *
         * Unsubscribe and disallow an email from being used.
         *
         * TODO Add something that lets the user who got their email revoked know that their email did get revoked.
         */
        get("/unsubscribe") {
            val params = call.request.queryParameters
            val email = params["email"]
            val verify = params["verify"]

            if (email == null || verify == null)
                throw InvalidArguments("email", "verify")

            val rs = DatabaseHandler.getConnection()
                    .prepareStatement("SELECT id FROM verify WHERE verify = ?")
                    .apply { setString(1, verify) }
                    .executeQuery()

            if (rs.next()) {
                val id = rs.getLong("id")
                val user = UserManager.getUser(id)

                if (user.email == email) {
                    user.verified = false
//                    user.updateEmail("") // TODO

                    DatabaseHandler.getConnection()
                            .prepareStatement("DELETE FROM verify WHERE id = ?")
                            .apply { setLong(1, id) }
                            .executeUpdate()

                    DatabaseHandler.getConnection()
                            .prepareStatement("INSERT INTO unsubscribed (email) VALUES (?)")
                            .apply { setString(1, email) }
                            .executeUpdate()

                    call.respond(HttpStatusCode.OK, Response("Email successfully unsubscribed. Your email will no longer be able to be used to register an account with."))
                    return@get
                }
            }

            throw InvalidArguments("verify", "email")
        }

        /**
         * Change your own name.
         *
         * TODO check username already exists
         */
        put("/name") {
            val token = call.isAuthenticated()

            val params = call.receiveParameters()
            val username = params["username"]

            if (username == null)
                call.respond(HttpStatusCode.BadRequest, Response("No username parameter"))
            else {
                when {
                    username.length > 16 ->
                        call.respond(HttpStatusCode.BadRequest, Response("Username is too long! (must be <=16)"))

                    3 > username.length ->
                        call.respond(HttpStatusCode.BadRequest, Response("Username is too short! (must be >3)"))

                    else -> {
                        UserManager.getUser(token.owner).username = username

                        call.respond(HttpStatusCode.OK, Response("Changed username."))
                    }
                }
            }
        }

        /**
         * Change your own picture;.
         */
        put("/picture") {
            val token = call.isAuthenticated()

            if (call.request.header("Content-Type") == ContentType.Image.JPEG.toString()) {
                val bytes = call.receiveStream().readBytes()

                if (bytes.size > 4000000) {
                    call.respond(HttpStatusCode.PayloadTooLarge, Response("That picture is too big!"))
                } else {
                    ProfilePictureManager.uploadPicture(token.owner, bytes)

                    call.respond(Response("Uploaded picture successfully"))
                }
                return@put
            }

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

                if (name == null)
                    call.respond(HttpStatusCode.BadRequest, Response("No name parameter"))
                else {
                    call.respondBytes(ProfilePictureManager.getPicture(UserManager.getId(name)), ContentType.Image.JPEG)
                }
            }
        }

        /**
         * Manage other users using IDs.
         */
        route("/id/{id}") {
            get {
                val id = call.parameters["id"]?.toLongOrNull()

                if (id == null)
                    call.respond(HttpStatusCode.BadRequest, Response("No id parameter"))
                else
                    call.respond(Response(UserManager.getUser(id)))
            }

            get("/picture") {
                val id = call.parameters["id"]?.toLongOrNull()

                if (id == null)
                    call.respond(HttpStatusCode.BadRequest, Response("No id parameter"))
                else {
                    call.respondBytes(ProfilePictureManager.getPicture(UserManager.getUser(id).id), ContentType.Image.JPEG)
                }
            }
        }
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
                call.respond(AuthenticateResponse(auth, UserManager.getUser(auth.owner)))
            }
        }
    }
}