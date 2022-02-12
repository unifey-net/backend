package net.unifey.handle.admin

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import net.unifey.auth.Authenticator
import net.unifey.auth.isAuthenticated
import net.unifey.handle.InvalidArguments
import net.unifey.handle.communities.Community
import net.unifey.handle.feeds.FeedManager
import net.unifey.handle.feeds.posts.Post
import net.unifey.handle.mongo.MONGO
import net.unifey.handle.users.GlobalRoles
import net.unifey.handle.users.User
import net.unifey.handle.users.UserInputRequirements
import net.unifey.handle.users.UserManager
import net.unifey.handle.users.member.Member
import net.unifey.handle.users.profile.Profile
import net.unifey.handle.users.profile.cosmetics.Cosmetics
import org.litote.kmongo.eq
import org.mindrot.jbcrypt.BCrypt

/**
 * The type of statistics that are viewable through the endpoint /admin/stats.
 *
 * @param requiredArguments The required arguments to get the statistics.
 * @param getStatistic The function to get the statistic. Provides the arguments previously stated
 * in [requiredArguments].
 */
private enum class StatsViewType(
    val requiredArguments: List<String>,
    val getStatistic: suspend (Map<String, String>) -> Any
) {
    /** The count of the currently signed up users. */
    USER_COUNT(
        emptyList(),
        { MONGO.getDatabase("users").getCollection<User>("users").countDocuments() }
    ),

    /** The amount of created communities. */
    COMMUNITY_COUNT(
        emptyList(),
        {
            MONGO
                .getDatabase("communities")
                .getCollection<Community>("communities")
                .countDocuments()
        }
    ),

    /** The amount of posts in a feed. */
    POST_COUNT(
        listOf("feed"),
        { args ->
            MONGO
                .getDatabase("feeds")
                .getCollection<Post>("posts")
                .countDocuments(Post::feed eq args["feed"])
        }
    )
}

/** A view stats request from /admin/stats */
@Serializable private data class ViewStatsResponse(val statType: String, val stat: String)

/** Endpoint: /admin/stats */
private suspend fun viewStats(call: ApplicationCall) {
    call.isAuthenticated(permissionLevel = GlobalRoles.ADMIN)

    val typeStr = call.request.queryParameters["type"] ?: throw InvalidArguments("type")
    val type =
        StatsViewType.values().singleOrNull { type -> type.name.equals(typeStr, true) }
            ?: throw InvalidArguments("type")

    val args = hashMapOf<String, String>()

    type.requiredArguments.forEach { arg ->
        if (!call.request.queryParameters.contains(arg)) {
            throw InvalidArguments(arg)
        }

        args[arg] = call.request.queryParameters[arg]!!
    }

    call.respond(ViewStatsResponse(type.name, type.getStatistic.invoke(args).toString()))
}

fun Routing.adminPages() =
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
                    listOf(
                        Cosmetics.getAll().single { cosmetic ->
                            cosmetic.id.equals("Debug_Account", true)
                        }
                    )
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
