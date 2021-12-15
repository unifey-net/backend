package net.unifey.handle.communities.routing

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import net.unifey.auth.AuthenticationException
import net.unifey.auth.isAuthenticated
import net.unifey.auth.tokens.Token
import net.unifey.handle.Error
import net.unifey.handle.InvalidArguments
import net.unifey.handle.NoPermission
import net.unifey.handle.NotFound
import net.unifey.handle.communities.Community
import net.unifey.handle.communities.CommunityManager
import net.unifey.handle.communities.CommunityRoles
import net.unifey.handle.communities.responses.GetCommunityResponse
import net.unifey.handle.emotes.EmoteHandler
import net.unifey.handle.users.User
import net.unifey.handle.users.UserManager
import net.unifey.response.Response
import net.unifey.util.cleanInput
import org.mindrot.jbcrypt.BCrypt

/**
 * Properly respond [community] according to the authentication.
 */
suspend fun ApplicationCall.respondCommunity(community: Community) {
    val user = try {
        isAuthenticated()
    } catch (authEx: AuthenticationException) {
        null
    }

    if (community.viewRole != CommunityRoles.DEFAULT) {
        if (user == null || !CommunityRoles.hasPermission(community.getRole(user.owner), community.viewRole)) {
            respond(
                GetCommunityResponse(
                    community,
                    community.getRole(user?.owner ?: -1),
                    listOf(),
                    null
                )
            )

            return
        }
    }

    respond(
        GetCommunityResponse(
            community,
            community.getRole(user?.owner ?: -1L),
            EmoteHandler.getCommunityEmotes(community),
            community.getFeed()
        )
    )
}

/**
 * For either modifying the name of description of a community.
 *
 * @param param The parameter name. (Ex: name)
 * @param permission The required permission level (Ex: Admin)
 * @param requirePassword If the password should be included for security.
 */
@Throws(NotFound::class, InvalidArguments::class, NoPermission::class)
suspend fun ApplicationCall.modifyCommunity(
    param: String,
    permission: Int,
    requirePassword: Boolean = false
): Pair<Community, String> {
    val token = isAuthenticated()

    val id = parameters["id"]?.toLongOrNull()
        ?: throw InvalidArguments("p_id")

    val community = CommunityManager.getCommunityById(id)

    if (permission > community.getRole(token.owner))
        throw NoPermission()

    val params = receiveParameters()

    val par = params[param]
        ?: throw InvalidArguments(param)

    if (requirePassword) {
        val password = params["password"]
            ?: throw InvalidArguments("password")

        if (!BCrypt.checkpw(password, token.getOwner().password))
            throw Error({
                respond(HttpStatusCode.Unauthorized, Response("Invalid password!"))
            })
    }

    return community to cleanInput(par)
}

/**
 * Modify a role.
 */
@Throws(NotFound::class, InvalidArguments::class, NoPermission::class)
suspend fun ApplicationCall.modifyRole(): Pair<Community, Int> {
    val (community, roleStr) = modifyCommunity("role", CommunityRoles.ADMIN, false)

    val role = roleStr.toIntOrNull()

    if (role == null || !CommunityRoles.isValid(role))
        throw InvalidArguments("role")

    return community to role
}

/**
 * Get a rule.
 */
@Throws(NoPermission::class)
suspend fun ApplicationCall.getCommunityRule(): Pair<Community, Token> {
    val token = isAuthenticated()

    val id = parameters["id"]?.toLongOrNull()
        ?: throw InvalidArguments("p_id")

    val community = CommunityManager.getCommunityById(id)

    if (!CommunityRoles.hasPermission(community.getRole(token.owner), CommunityRoles.ADMIN))
        throw NoPermission()

    return community to token
}

/**
 * Manage personal communities.
 */
@Throws(NotFound::class, InvalidArguments::class)
suspend fun ApplicationCall.managePersonalCommunities(): Pair<User, Community> {
    val token = isAuthenticated()
    val user = UserManager.getUser(token.owner)

    val params = receiveParameters()

    val id = params["id"]?.toLongOrNull()
        ?: throw InvalidArguments("id")

    return user to CommunityManager.getCommunityById(id)
}
