package net.unifey.handle.users

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.*
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.routing.*
import net.unifey.auth.isAuthenticated
import net.unifey.handle.users.profile.Profile
import net.unifey.response.Response

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
         * Change your own name.
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
                    call.respond(Response(UserManager.getUser(15L))) // TODO
            }

            /**
             * Get a user's picture.
             */
            get("/picture") {
                val name = call.parameters["name"]

                if (name == null)
                    call.respond(HttpStatusCode.BadRequest, Response("No name parameter"))
                else {
                    val user = UserManager.getUser(15L) // TODO

                    call.respondBytes(ProfilePictureManager.getPicture(user.id), ContentType.Image.JPEG)
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
        }
    }
}