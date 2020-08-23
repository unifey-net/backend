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
import net.unifey.handle.NoPermission
import net.unifey.handle.NotFound
import net.unifey.handle.S3ImageHandler
import net.unifey.handle.users.email.Unverified
import net.unifey.handle.users.email.UserEmailManager
import net.unifey.handle.users.profile.Profile
import net.unifey.handle.users.profile.cosmetics.Cosmetics
import net.unifey.util.ensureProperImageBody
import net.unifey.handle.users.responses.AuthenticateResponse
import net.unifey.response.Response
import net.unifey.util.cleanInput
import org.mindrot.jbcrypt.BCrypt

fun Routing.userPages() {
    route("/user") {
        route("/cosmetic") {
            /**
             * Helper function for cosmetic management calls.
             * Returns the type to the ID.
             */
            fun ApplicationCall.manageCosmetic(): Triple<Int, String, String?> {
                val token = isAuthenticated()

                if (UserManager.getUser(token.owner).role != GlobalRoles.ADMIN)
                    throw NoPermission()

                val params = request.queryParameters

                val id = params["id"]
                val type = params["type"]?.toIntOrNull()

                if (id == null || type == null)
                    throw InvalidArguments("type", "id")

                return Triple(type, cleanInput(id), params["desc"])
            }

            /**
             * Get an image cosmetic's image.
             */
            get("/viewer") {
                val params = call.request.queryParameters

                val type = params["type"]?.toIntOrNull()
                val id = params["id"]

                if (type == null || id == null)
                    throw InvalidArguments("type", "id")

                call.respondBytes(S3ImageHandler.getPicture("cosmetics/$type.$id.jpg", "cosmetics/default.jpg"))
            }

            /**
             * Get all cosmetics, or select by type or id. To access a user's cosmetics, get their profile which contains their cosmetics.
             */
            get {
                val params = call.request.queryParameters

                val id = params["id"]
                val type = params["type"]?.toIntOrNull()

                val cosmetics = when {
                    id != null && type != null -> Cosmetics.getAll().filter { cos -> cos.id.equals(id, true) && cos.type == type }
                    id != null -> Cosmetics.getAll().filter { cos -> cos.id.equals(id, true) }
                    type != null -> Cosmetics.getAll().filter { cos -> cos.type == type }

                    else -> Cosmetics.getAll()
                }

                call.respond(cosmetics)
            }

            /**
             * Toggle a cosmetic for a user.
             */
            post {
                val token = call.isAuthenticated()

                if (UserManager.getUser(token.owner).role != GlobalRoles.ADMIN)
                    throw NoPermission()

                val params = call.receiveParameters()

                val user = params["user"]?.toLongOrNull()
                val id = params["id"]
                val type = params["type"]?.toIntOrNull()

                if (id == null || type == null || user == null)
                    throw InvalidArguments("type", "id", "user")

                val userObj = UserManager.getUser(user)

                val retrieved = Cosmetics.getAll()
                        .firstOrNull { cosmetic -> cosmetic.type == type && cosmetic.id.equals(id, true) }
                        ?: throw NotFound("cosmetic")

                val newCosmetics = userObj.profile.cosmetics.toMutableList()

                if (newCosmetics.any { cos -> cos.id.equals(id, true) && cos.type == type })
                    newCosmetics.removeIf { cos -> cos.id.equals(id, true) && cos.type == type }
                else
                    newCosmetics.add(retrieved)

                userObj.profile.cosmetics = newCosmetics

                call.respond(Response())
            }

            /**
             * Create a cosmetic
             */
            put {
                val (type, id, desc) = call.manageCosmetic()

                if (desc == null)
                    throw InvalidArguments("desc")

                when (type) {
                    0 -> {
                        val badge = call.ensureProperImageBody()

                        S3ImageHandler.upload("cosmetics/${type}.${id}.jpg", badge)
                    }
                }

                Cosmetics.uploadCosmetic(type, id, desc)

                call.respond(Response())
            }

            /**
             * Delete a cosmetic
             */
            delete {
                val (type, id) = call.manageCosmetic()

                when (type) {
                    0 -> {
                        S3ImageHandler.delete("cosmetics/${type}.${id}.jpg")
                    }
                }

                Cosmetics.deleteCosmetic(type, id)

                call.respond(Response())
            }
        }

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
         * Manage your profile
         */
        route("/profile") {
            /**
             * Get [paramName] for a profile action.
             */
            @Throws(InvalidArguments::class)
            suspend fun ApplicationCall.profileInput(paramName: String, maxLength: Int): Pair<User, String> {
                val token = isAuthenticated()

                val params = receiveParameters()

                val param = params[paramName] ?: throw InvalidArguments(paramName)

                val properParam = cleanInput(param)

                if (properParam.length > maxLength || properParam.isBlank())
                    throw InvalidArguments(paramName)

                return UserManager.getUser(token.owner) to param
            }

            /**
             * Change the description
             */
            put("/description") {
                val (user, desc) = call.profileInput("description", Profile.MAX_DESC_LEN)

                user.profile.description = desc

                call.respond(Response())
            }

            /**
             * Change the location
             */
            put("/location") {
                val (user, loc) = call.profileInput("location", Profile.MAX_LOC_LEN)

                user.profile.location = loc

                call.respond(Response())
            }

            /**
             * Change the discord
             */
            put("/discord") {
                val (user, disc) = call.profileInput("discord", Profile.MAX_DISC_LEN)

                user.profile.discord = disc

                call.respond(Response())
            }
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
        val remember = params["remember"]?.toBoolean()

        if (username == null || password == null || remember == null)
            throw InvalidArguments("username", "password", "remember")

        val auth = Authenticator.generateIfCorrect(username, password, remember)

        if (auth == null)
            call.respond(HttpStatusCode.Unauthorized, Response("Invalid credentials."))
        else {
            call.respond(AuthenticateResponse(auth, UserManager.getUser(auth.owner)))
        }
    }
}