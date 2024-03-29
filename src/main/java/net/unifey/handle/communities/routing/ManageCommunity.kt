package net.unifey.handle.communities.routing

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Refill
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import net.unifey.auth.AuthenticationException
import net.unifey.auth.isAuthenticated
import net.unifey.handle.InvalidArguments
import net.unifey.handle.NoPermission
import net.unifey.handle.S3ImageHandler
import net.unifey.handle.communities.*
import net.unifey.handle.communities.rules.CommunityRuleManager
import net.unifey.handle.communities.rules.RuleInputRequirements
import net.unifey.handle.users.User
import net.unifey.handle.users.UserManager
import net.unifey.handle.users.member.MemberManager.isMemberOf
import net.unifey.response.Response
import net.unifey.util.FieldChangeLimiter
import net.unifey.util.FieldChangeLimiter.checkLimited
import net.unifey.util.PageRateLimit
import net.unifey.util.clean
import net.unifey.util.ensureProperImageBody

val MANAGE_COMMUNITY: Route.() -> Unit = {
    /** Get a community by it's ID */
    get {
        val id = call.parameters["id"]?.toLongOrNull() ?: throw InvalidArguments("p_id")

        val community = CommunityManager.getCommunityById(id)

        call.respondCommunity(community)
    }

    /** Get a communities staff members */
    get("/staff") {
        @Serializable data class CommunityStaffResponse(val role: Int, val user: User)

        val user =
            try {
                call.isAuthenticated()
            } catch (authEx: AuthenticationException) {
                null
            }

        val id = call.parameters["id"]?.toLongOrNull() ?: throw InvalidArguments("p_id")

        val community = CommunityManager.getCommunityById(id)

        if (community.permissions.viewRole != CommunityRoles.DEFAULT &&
                (user == null || community.permissions.viewRole > community.getRole(user.owner))
        )
            throw NoPermission()

        call.respond(
            community.roles.map { (id, role) ->
                CommunityStaffResponse(role, UserManager.getUser(id))
            }
        )
    }

    /** Delete a community. */
    delete {
        val token = call.isAuthenticated()

        val id = call.parameters["id"]?.toLongOrNull() ?: throw InvalidArguments("p_id")

        val community = CommunityManager.getCommunityById(id)

        if (community.getRole(token.owner) != CommunityRoles.OWNER) throw NoPermission()

        CommunityManager.deleteCommunity(id)

        call.respond(Response("OK"))
    }

    /** Manage a communities rules. */
    route("/rules") {
        /** Create a rule. */
        put {
            val (community, _) = call.getCommunityRule()

            val params = call.receiveParameters()

            val title = params["title"].clean()
            val body = params["body"].clean()

            if (title == null || body == null) throw InvalidArguments("title", "body")

            RuleInputRequirements.meets(
                listOf(body to RuleInputRequirements.BODY, title to RuleInputRequirements.TITLE)
            )

            val id = CommunityRuleManager.createRule(title, body, community)

            call.respond(Response(id))
        }

        /** Delete a rule. */
        delete("/{rule}") {
            val (community, _) = call.getCommunityRule()

            val id = call.parameters["rule"]?.toLongOrNull() ?: throw InvalidArguments("p_rule")

            CommunityRuleManager.deleteRule(id, community)

            call.respond(Response("OK"))
        }

        /** Update the rule's body. */
        patch("/body") {
            val (community, _) = call.getCommunityRule()

            val params = call.receiveParameters()

            val id = params["id"]?.toLongOrNull()
            val body = params["body"].clean()

            if (id == null || body == null) throw InvalidArguments("body", "id")

            RuleInputRequirements.meets(body, RuleInputRequirements.BODY)

            CommunityRuleManager.modifyBody(body, id, community)

            call.respond(Response("OK"))
        }

        /** Update the rule's title. */
        patch("/title") {
            val (community, _) = call.getCommunityRule()

            val params = call.receiveParameters()

            val id = params["id"]?.toLongOrNull()
            val title = params["title"].clean()

            if (id == null || title == null) throw InvalidArguments("title", "id")

            RuleInputRequirements.meets(title, RuleInputRequirements.TITLE)

            CommunityRuleManager.modifyTitle(title, id, community)

            call.respond(Response("OK"))
        }
    }

    /** Verify a name to be apart of a community. */
    get("/verifyname") {
        call.isAuthenticated()

        val id = call.parameters["id"]?.toLongOrNull() ?: throw InvalidArguments("p_id")

        val community = CommunityManager.getCommunityById(id) // verify it exists

        val name = call.request.queryParameters["name"] ?: throw InvalidArguments("q_name")

        call.respond(
            Response(
                CommunityManager.getMembers(community.id)
                    .singleOrNull { user -> user.username.equals(name, true) }
                    ?.id
            )
        )
    }

    val searchBucket = PageRateLimit(Bandwidth.classic(5, Refill.greedy(1, Duration.ofSeconds(5))))

    /** Search members */
    post("/search") {
        val token = call.isAuthenticated(pageRateLimit = searchBucket)

        val id = call.parameters["id"]?.toLongOrNull() ?: throw InvalidArguments("p_id")

        val community = CommunityManager.getCommunityById(id)

        if (!CommunityRoles.hasPermission(community.getRole(token.owner), CommunityRoles.MODERATOR))
            throw NoPermission()

        val params = call.receiveParameters()

        val name = params["name"] ?: ""

        call.respond(
            CommunityManager.getMembers(community.id).filter { user ->
                user.username.contains(name, true)
            }
        )
    }

    /** Manage a communities' profile picture. */
    route("/picture") {
        get {
            call.isAuthenticated()

            val id = call.parameters["id"]?.toLongOrNull() ?: throw InvalidArguments("p_id")

            val community = CommunityManager.getCommunityById(id)

            call.respondBytes(ContentType.Image.JPEG, HttpStatusCode.OK) {
                S3ImageHandler.getPicture("community/${community.id}.jpg", "community/default.jpg")
            }
        }

        /** Update a communities' picture. */
        put {
            val token = call.isAuthenticated()

            val id = call.parameters["id"]?.toLongOrNull() ?: throw InvalidArguments("p_id")

            val community = CommunityManager.getCommunityById(id)

            if (CommunityRoles.ADMIN > community.getRole(token.owner))
                throw NoPermission()

            val image = call.ensureProperImageBody()

            S3ImageHandler.upload("community/${community.id}.jpg", image)

            call.respond(Response("OK"))
        }
    }

    /** The length between name changes. */
    val nameChangeLength = TimeUnit.DAYS.toMillis(31)

    /** Change the name. */
    put("/name") {
        val (community, name) = call.modifyCommunity("name", CommunityRoles.ADMIN, true)

        checkLimited("COMMUNITY", community.id, "NAME")

        CommunityInputRequirements.meets(name, CommunityInputRequirements.NAME)
        CommunityManager.changeName(community.id, name)

        FieldChangeLimiter.createLimit(
            "COMMUNITY",
            community.id,
            "NAME",
            System.currentTimeMillis() + nameChangeLength
        )

        call.respond(Response("OK"))
    }

    /** Change the description. */
    put("/description") {
        val (community, desc) = call.modifyCommunity("description", CommunityRoles.ADMIN)

        CommunityInputRequirements.meets(desc, CommunityInputRequirements.DESCRIPTION)

        CommunityManager.changeDescription(community.id, desc)

        call.respond(Response("OK"))
    }

    /** Manage a communities roles. Also manages post/view permissions. */
    route("/roles") {
        /** Get a communities roles. */
        get {
            @Serializable
            data class UserCommunityRole(val id: Long, val name: String, val role: Int)

            val token = call.isAuthenticated()

            val id = call.parameters["id"]?.toLongOrNull() ?: throw InvalidArguments("p_id")

            val community = CommunityManager.getCommunityById(id)

            if (CommunityRoles.MODERATOR > community.getRole(token.owner)) throw NoPermission()

            val response =
                community.roles.map { role ->
                    UserCommunityRole(role.key, UserManager.getUser(role.key).username, role.value)
                }

            call.respond(response)
        }

        /** Change the view permissions. */
        put("/view") {
            val (community, role) = call.modifyRole()

            community.setPermissions(viewRole = role)

            call.respond(Response("OK"))
        }

        /** Change the comment permissions. */
        put("/comment") {
            val (community, role) = call.modifyRole()

            community.setPermissions(commentRole = role)

            call.respond(Response("OK"))
        }

        /** Change the post permissions. */
        put("/post") {
            val (community, role) = call.modifyRole()

            community.setPermissions(postRole = role)

            call.respond(Response("OK"))
        }

        /** Change a user's role. */
        post {
            val id = call.parameters["id"]?.toLongOrNull() ?: throw InvalidArguments("p_id")

            val community = CommunityManager.getCommunityById(id)

            val token = call.isAuthenticated()
            val selfRole = community.getRole(token.owner)

            val params = call.receiveParameters()

            val target = params["target"]?.toLongOrNull()
            val role = params["role"]?.toIntOrNull()

            if (target == null || role == null) throw InvalidArguments("target", "role")

            val targetUser = UserManager.getUser(target)

            if (!targetUser.isMemberOf(community.id)) throw NoPermission()

            when {
                targetUser.id == token.owner -> throw NoPermission()

                // only the owner can grant other users administrator
                CommunityRoles.ADMIN == role && selfRole != CommunityRoles.OWNER ->
                    throw NoPermission()

                // TODO allow owners to give their ownership away.
                CommunityRoles.OWNER == role -> throw NoPermission()

                // The only roles that can be given/managed by a community is moderator and
                // administrator.
                !(1..3).contains(role) -> throw InvalidArguments("role")
            }

            if (role == 1) {
                community.removeRole(targetUser.id)
            } else {
                community.setRole(targetUser.id, role)
            }

            call.respond(Response("OK"))
        }
    }
}
