package net.unifey.handle.admin.pages

import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import net.unifey.auth.isAuthenticated
import net.unifey.handle.InvalidArguments
import net.unifey.handle.communities.Community
import net.unifey.handle.feeds.posts.Post
import net.unifey.handle.mongo.MONGO
import net.unifey.handle.users.GlobalRoles
import net.unifey.handle.users.User
import org.litote.kmongo.eq

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
@Serializable
private data class ViewStatsResponse(val statType: String, val stat: String)

/** Endpoint: /admin/stats */
suspend fun viewStats(call: ApplicationCall) {
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