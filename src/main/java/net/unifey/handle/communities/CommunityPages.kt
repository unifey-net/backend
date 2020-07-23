package net.unifey.handle.communities

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
import net.unifey.handle.*
import net.unifey.handle.communities.responses.GetCommunityResponse
import net.unifey.handle.emotes.EmoteHandler
import net.unifey.handle.users.User
import net.unifey.handle.users.UserManager
import net.unifey.response.Response
import net.unifey.util.cleanInput
import net.unifey.util.ensureProperImageBody

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
            get {
                val id = call.parameters["id"]?.toLongOrNull()
                        ?: throw InvalidArguments("p_id")

                val community = CommunityManager.getCommunityById(id)

                call.respondCommunity(community)
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
             */
            @Throws(NotFound::class, InvalidArguments::class, NoPermission::class)
            suspend fun ApplicationCall.modifyCommunity(param: String, permission: Int): Pair<Community, String> {
                val token = isAuthenticated()

                val id = parameters["id"]?.toLongOrNull()
                        ?: throw InvalidArguments("p_id")

                val community = CommunityManager.getCommunityById(id)

                if (permission > community.getRole(token.owner) ?: CommunityRoles.DEFAULT)
                    throw NoPermission()

                val params = receiveParameters()

                val par = params[param]
                        ?: throw InvalidArguments(param)

                return community to cleanInput(par)
            }

            /**
             * Change the name.
             */
            put("/name") {
                val (community, name) = call.modifyCommunity("name", CommunityRoles.ADMIN)

                InputRequirements.nameMeets(name)

                community.name = name

                call.respond(Response())
            }

            /**
             * Change the description.
             */
            put("/description") {
                val (community, desc) = call.modifyCommunity("description", CommunityRoles.ADMIN)

                InputRequirements.descMeets(desc)

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
                    val (community, roleStr) = modifyCommunity("role", CommunityRoles.ADMIN)

                    val role = roleStr.toIntOrNull()

                    if (role == null || !CommunityRoles.isValid(role))
                        throw InvalidArguments("role")

                    return community to role
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

            InputRequirements.nameMeets(name)
            InputRequirements.descMeets(desc)

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