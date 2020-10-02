package net.unifey.handle.communities

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Refill
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.routing.*
import net.unifey.auth.ex.AuthenticationException
import net.unifey.auth.isAuthenticated
import net.unifey.auth.tokens.Token
import net.unifey.handle.*
import net.unifey.handle.communities.responses.GetCommunityResponse
import net.unifey.handle.communities.rules.CommunityRuleManager
import net.unifey.handle.communities.rules.RuleInputRequirements
import net.unifey.handle.emotes.EmoteHandler
import net.unifey.handle.users.User
import net.unifey.handle.users.UserManager
import net.unifey.response.Response
import net.unifey.util.PageRateLimit
import net.unifey.util.clean
import net.unifey.util.cleanInput
import net.unifey.util.ensureProperImageBody
import org.mindrot.jbcrypt.BCrypt
import java.time.Duration

fun Routing.communityPages() {
    route("/community") {
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
                if (user == null || community.getRole(user.owner) != community.viewRole) {
                    respond(GetCommunityResponse(
                            community,
                            community.getRole(user?.owner ?: -1),
                            null,
                            null
                    ))

                    return
                }
            }

            respond(GetCommunityResponse(
                    community,
                    community.getRole(user?.owner ?: -1L),
                    EmoteHandler.getCommunityEmotes(community),
                    community.getFeed()
            ))
        }

        /**
         * Manage your personal communities. (joined communities)
         */
        route("/manage") {
            /**
             * Manage personal communities.
             */
            @Throws(NotFound::class, InvalidArguments::class)
            suspend fun ApplicationCall.managePersonal(): Pair<User, Community> {
                val token = isAuthenticated()
                val user = UserManager.getUser(token.owner)

                val params = receiveParameters()

                val id = params["id"]?.toLongOrNull()
                        ?: throw InvalidArguments("id")

                return user to CommunityManager.getCommunityById(id)
            }

            /**
             * Join a community.
             */
            put {
                val (user, community) = call.managePersonal()

                if (!user.member.isMemberOf(community.id)) {
                    user.member.join(community.id)
                    call.respond(Response())
                } else
                    throw AlreadyExists("community", community.id.toString())
            }

            /**
             * Leave a community.
             */
            delete {
                val (user, community) = call.managePersonal()

                if (community.getRole(user.id) == CommunityRoles.OWNER)
                    throw NoPermission()

                if (user.member.isMemberOf(community.id)) {
                    user.member.leave(community.id)
                    call.respond(Response())
                } else
                    throw NotFound("community")
            }
        }

        /**
         * Get a community by it's name.
         */
        get("/name/{name}") {
            val name = call.parameters["name"]
                    ?: throw InvalidArguments("p_name")

            val community = CommunityManager.getCommunityByName(name)

            call.respondCommunity(community)
        }

        route("/{id}") {
            /**
             * Get a community by it's ID
             */
            get {
                val id = call.parameters["id"]?.toLongOrNull()
                        ?: throw InvalidArguments("p_id")

                val community = CommunityManager.getCommunityById(id)

                call.respondCommunity(community)
            }

            /**
             * Get a communities staff members
             */
            get("/staff") {
                data class CommunityStaffResponse(
                        val role: Int,
                        val user: User
                )

                val user = try {
                    call.isAuthenticated()
                } catch (authEx: AuthenticationException) {
                    null
                }

                val id = call.parameters["id"]?.toLongOrNull()
                        ?: throw InvalidArguments("p_id")

                val community = CommunityManager.getCommunityById(id)

                if (community.viewRole != CommunityRoles.DEFAULT && (user == null || community.viewRole > community.getRole(user.owner)))
                    throw NoPermission()

                call.respond(
                        community.roles.map { (id, role) ->
                            CommunityStaffResponse(role, UserManager.getUser(id))
                        }
                )
            }


            /**
             * Delete a community.
             */
            delete {
                val token = call.isAuthenticated()

                val id = call.parameters["id"]?.toLongOrNull()
                        ?: throw InvalidArguments("p_id")

                val community = CommunityManager.getCommunityById(id)

                if (community.getRole(token.owner) != CommunityRoles.OWNER)
                    throw NoPermission()

                CommunityManager.deleteCommunity(id)

                call.respond(Response())
            }

            /**
             * Manage a communities rules.
             */
            route("/rules") {
                @Throws(NoPermission::class)
                fun ApplicationCall.getRule(): Pair<Community, Token> {
                    val token = isAuthenticated()

                    val id = parameters["id"]?.toLongOrNull()
                            ?: throw InvalidArguments("p_id")

                    val community = CommunityManager.getCommunityById(id)

                    if (!CommunityRoles.hasPermission(community.getRole(token.owner), CommunityRoles.ADMIN))
                        throw NoPermission()

                    return community to token
                }

                /**
                 * Create a rule.
                 */
                put {
                    val (community) = call.getRule()

                    val params = call.receiveParameters()

                    val title = params["title"].clean()
                    val body = params["body"].clean()

                    if (title == null || body == null)
                        throw InvalidArguments("title", "body")

                    RuleInputRequirements.meets(listOf(
                            body to RuleInputRequirements.BODY,
                            title to RuleInputRequirements.TITLE
                    ))

                    val id = CommunityRuleManager.createRule(title, body, community)

                    call.respond(Response(id))
                }

                /**
                 * Delete a rule.
                 */
                delete("/{rule}") {
                    val (community) = call.getRule()

                    val id = call.parameters["rule"]?.toLongOrNull()
                            ?: throw InvalidArguments("p_rule")

                    CommunityRuleManager.deleteRule(id, community)

                    call.respond(Response())
                }

                /**
                 * Update the rule's body.
                 */
                patch("/body") {
                    val (community) = call.getRule()

                    val params = call.receiveParameters()

                    val id = params["id"]?.toLongOrNull()
                    val body = params["body"].clean()

                    if (id == null || body == null)
                        throw InvalidArguments("body", "id")

                    RuleInputRequirements.meets(body, RuleInputRequirements.BODY)

                    CommunityRuleManager.modifyBody(body, id, community)

                    call.respond(Response())
                }

                /**
                 * Update the rule's title.
                 */
                patch("/title") {
                    val (community) = call.getRule()

                    val params = call.receiveParameters()

                    val id = params["id"]?.toLongOrNull()
                    val title = params["title"].clean()

                    if (id == null || title == null)
                        throw InvalidArguments("title", "id")

                    RuleInputRequirements.meets(title, RuleInputRequirements.TITLE)

                    CommunityRuleManager.modifyTitle(title, id, community)

                    call.respond(Response())
                }
            }

            /**
             * Verify a name to be apart of a community.
             */
            get("/verifyname") {
                call.isAuthenticated()

                val id = call.parameters["id"]?.toLongOrNull()
                        ?: throw InvalidArguments("p_id")

                val community = CommunityManager.getCommunityById(id) // verify it exists

                val name = call.request.queryParameters["name"]
                        ?: throw InvalidArguments("q_name")

                call.respond(
                        Response(
                                CommunityManager
                                        .getMembersAsync(community.id)
                                        .await()
                                        .singleOrNull { user -> user.username.equals(name, true) }
                                        ?.id
                        )
                )
            }

            /**
             * Search members
             */
            post("/search") {
                val token = call.isAuthenticated(pageRateLimit = PageRateLimit(Bandwidth.classic(
                        5,
                        Refill.greedy(1, Duration.ofSeconds(1))
                )))

                val id = call.parameters["id"]?.toLongOrNull()
                        ?: throw InvalidArguments("p_id")

                val community = CommunityManager.getCommunityById(id)

                if (!CommunityRoles.hasPermission(community.getRole(token.owner), CommunityRoles.MODERATOR))
                    throw NoPermission()

                val params = call.receiveParameters()

                val name = params["name"] ?: ""

                call.respond(
                        CommunityManager
                                .getMembersAsync(community.id)
                                .await()
                                .filter { user -> user.username.contains(name, true) }
                )
            }

            /**
             * Manage a communities' profile picture.
             */
            route("/picture") {
                get {
                    call.isAuthenticated()

                    val id = call.parameters["id"]?.toLongOrNull()
                            ?: throw InvalidArguments("p_id")

                    val community = CommunityManager.getCommunityById(id)

                    call.respondBytes(ContentType.Image.JPEG, HttpStatusCode.OK) {
                        S3ImageHandler.getPicture("community/${community.id}.jpg", "community/default.jpg")
                    }
                }

                /**
                 * Update a communities' picture.
                 */
                put {
                    val token = call.isAuthenticated()

                    val id = call.parameters["id"]?.toLongOrNull()
                            ?: throw InvalidArguments("p_id")

                    val community = CommunityManager.getCommunityById(id)

                    if (CommunityRoles.ADMIN > community.getRole(token.owner) ?: CommunityRoles.DEFAULT)
                        throw NoPermission()

                    val image = call.ensureProperImageBody()

                    S3ImageHandler.upload("community/${community.id}.jpg", image)

                    call.respond(Response())
                }
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
                        throw Error {
                            respond(HttpStatusCode.Unauthorized, Response("Invalid password!"))
                        }
                }

                return community to cleanInput(par)
            }

            /**
             * Change the name.
             */
            put("/name") {
                val (community, name) = call.modifyCommunity("name", CommunityRoles.ADMIN, true)

                CommunityInputRequirements.meets(name, CommunityInputRequirements.NAME)

                community.name = name

                call.respond(Response())
            }

            /**
             * Change the description.
             */
            put("/description") {
                val (community, desc) = call.modifyCommunity("description", CommunityRoles.ADMIN)

                CommunityInputRequirements.meets(desc, CommunityInputRequirements.DESCRIPTION)

                community.description = desc

                call.respond(Response())
            }

            /**
             * Manage a communities roles. Also manages post/view permissions.
             */
            route("/roles") {
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
                 * Get a communities roles.
                 */
                get {
                    val token = call.isAuthenticated()

                    val id = call.parameters["id"]?.toLongOrNull()
                            ?: throw InvalidArguments("p_id")

                    val community = CommunityManager.getCommunityById(id)

                    if (CommunityRoles.MODERATOR > community.getRole(token.owner))
                        throw NoPermission()

                    call.respond(community.roles)
                }

                /**
                 * Change the view permissions.
                 */
                put("/view") {
                    val (community, role) = call.modifyRole()

                    community.viewRole = role

                    call.respond(Response())
                }

                /**
                 * Change the comment permissions.
                 */
                put("/comment") {
                    val (community, role) = call.modifyRole()

                    community.commentRole = role

                    call.respond(Response())
                }

                /**
                 * Change the post permissions.
                 */
                put("/post") {
                    val (community, role) = call.modifyRole()

                    community.postRole = role

                    call.respond(Response())
                }

                /**
                 * Change a user's role.
                 */
                post {
                    val id = call.parameters["id"]?.toLongOrNull()
                            ?: throw InvalidArguments("p_id")

                    val community = CommunityManager.getCommunityById(id)

                    val token = call.isAuthenticated()
                    val selfRole = community.getRole(token.owner) ?: CommunityRoles.DEFAULT

                    val params = call.receiveParameters()

                    val target = params["target"]?.toLongOrNull()
                    val role = params["role"]?.toIntOrNull()

                    if (target == null || role == null)
                        throw InvalidArguments("target", "role")

                    val targetUser = UserManager.getUser(target)

                    if (!targetUser.member.isMemberOf(community.id))
                        throw NoPermission()

                    when {
                        targetUser.id == token.owner ->
                            throw NoPermission()

                        // only the owner can grant other users administrator
                        CommunityRoles.ADMIN == role && selfRole != CommunityRoles.OWNER ->
                            throw NoPermission()

                        // TODO allow owners to give their ownership away.
                        CommunityRoles.OWNER == role ->
                            throw NoPermission()

                        // The only roles that can be given/managed by a community is moderator and administrator.
                        !(1..3).contains(role) ->
                            throw InvalidArguments("role")
                    }

                    if (role == 1) {
                        if (targetUser.member.isMemberOf(community.id))
                            community.setRole(targetUser.id, CommunityRoles.MODERATOR)
                        else
                            community.removeRole(targetUser.id)
                    } else {
                        community.setRole(targetUser.id, role)
                    }

                    call.respond(Response())
                }
            }
        }

        /**
         * Create a community
         */
        put {
            val token = call.isAuthenticated()

            if (!CommunityManager.canCreate(token.owner))
                throw NoPermission()

            val params = call.receiveParameters()

            val name = params["name"]
            val desc = params["desc"]

            if (name == null || desc == null)
                throw InvalidArguments("name", "desc")

            CommunityInputRequirements.meets(listOf(
                    name to CommunityInputRequirements.NAME,
                    desc to CommunityInputRequirements.DESCRIPTION
            ))

            call.respond(CommunityManager.createCommunity(token.owner, name, desc))
        }

        /**
         * Get all communities
         */
        get {
            val params = call.request.queryParameters

            val page = params["page"]?.toIntOrNull() ?: 1

            call.respond(CommunityManager.getCommunities(page))
        }
    }
}