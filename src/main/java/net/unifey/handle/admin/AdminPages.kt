package net.unifey.handle.admin

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import net.unifey.auth.Authenticator
import net.unifey.auth.isAuthenticated
import net.unifey.handle.InvalidArguments
import net.unifey.handle.admin.pages.viewStats
import net.unifey.handle.feeds.FeedManager
import net.unifey.handle.mongo.MONGO
import net.unifey.handle.users.GlobalRoles
import net.unifey.handle.users.User
import net.unifey.handle.users.UserInputRequirements
import net.unifey.handle.users.UserManager
import net.unifey.handle.users.member.Member
import net.unifey.handle.users.profile.Profile
import org.mindrot.jbcrypt.BCrypt
import java.util.concurrent.TimeUnit

fun Route.adminPages() =
    route("/admin") {
        get("/stats") { viewStats(call) }

        /** Creates a debug account. */
        put("/debug-account") {
            @Serializable data class DebugAccountResponse(val token: String)

            call.isAuthenticated(permissionLevel = GlobalRoles.ADMIN)
            val args = call.receiveParameters()

            val name = args["name"] ?: throw InvalidArguments("name")
            val password = args["password"] ?: throw InvalidArguments("password")
            val role = args["level"]?.toIntOrNull() ?: GlobalRoles.DEFAULT

            UserInputRequirements.USERNAME_EXISTS.invoke(name)
            if (role > GlobalRoles.ADMIN) throw InvalidArguments("role")

            val id = UserManager.generateIdentifier()
            val user =
                User(
                    id = id,
                    username = name,
                    password = BCrypt.hashpw(password, BCrypt.gensalt()),
                    email = "$id@unifey.app",
                    verified = true,
                    role = role,
                    createdAt = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(14)
                )

            val profile =
                Profile(
                    id,
                    "A debug user.",
                    "Debug",
                    "Unifey HQ",
                    listOf()
                )

            val member = Member(id, listOf(), listOf())
            val db = MONGO.getDatabase("users")

            db.getCollection<Member>("members").insertOne(member)
            db.getCollection<Profile>("profiles").insertOne(profile)
            db.getCollection<User>("users").insertOne(user)

            FeedManager.createFeedForUser(id)

            call.respond(DebugAccountResponse(Authenticator.generate(id).token))
        }
    }
