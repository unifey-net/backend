package net.unifey.handle.reports

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import net.unifey.auth.isAuthenticated
import net.unifey.handle.Error
import net.unifey.handle.InvalidArguments
import net.unifey.handle.NoPermission
import net.unifey.handle.communities.CommunityManager
import net.unifey.handle.communities.CommunityRoles
import net.unifey.handle.communities.getRole
import net.unifey.handle.feeds.FeedManager
import net.unifey.handle.feeds.posts.PostManager
import net.unifey.handle.feeds.posts.comments.CommentManager
import net.unifey.handle.reports.obj.ReportType
import net.unifey.handle.users.GlobalRoles
import net.unifey.handle.users.UserManager
import net.unifey.response.Response
import net.unifey.util.clean

private fun getTarget(params: Parameters): Target {
    return Target.parse(params["targetId"], params["targetType"])
}

private fun getReason(params: Parameters): ReportType {
    return ReportType.values().singleOrNull { reportType ->
        reportType.toString().equals(params["reason"], true)
    }
        ?: throw InvalidArguments("reason")
}

private fun getReasonText(params: Parameters): String {
    return params["reasonText"]?.clean() ?: ""
}

/**
 * Ensure that [target] exists.
 *
 * Returns the feed if [target] is a post or comment.
 */
private suspend fun ensureExists(target: Target, reason: ReportType): String? {
    val id = target.id

    if (!reason.types.contains(target.type)) {
        throw InvalidArguments("target")
    }

    when (target.type) {
        TargetType.POST -> {
            val post = PostManager.getPost(id)

            return post.feed
        }
        TargetType.COMMENT -> {
            val comment = CommentManager.getCommentById(id)

            return comment.feed
        }
        TargetType.ACCOUNT -> {
            UserManager.getUser(id)

            return null
        }
    }
}

fun Routing.reportPages() {
    route("/report") {
        /** Create a report. */
        put {
            val token = call.isAuthenticated()

            if (ReportHandler.getReportsToday(token.owner) > ReportHandler.MAX_REPORT_PER_PERSON)
                throw Error({
                    respond(
                        HttpStatusCode.BadRequest,
                        Response("You can only report 3 times per day!")
                    )
                })

            val params = call.receiveParameters()

            val target = getTarget(params)
            val reason = getReason(params)

            val feed = ensureExists(target, reason)

            ReportHandler.addReport(target, reason, feed, token.owner, getReasonText(params))

            call.respond(Response("OK"))
        }

        /** Get all reports for feed. */
        route("/{feed}") {
            /** Get all reports intended for a feed. */
            get {
                val token = call.isAuthenticated()

                val feedId = call.parameters["feed"] ?: throw InvalidArguments("p_feed")

                val feed = FeedManager.getFeed(feedId)

                if (feed.id.startsWith("cf_")) {
                    val communityId = feed.id.removePrefix("cf_").toLong()

                    val community = CommunityManager.getCommunityById(communityId)

                    if (community.getRole(token.owner) < CommunityRoles.MODERATOR)
                        throw NoPermission()
                } else if (!feed.moderators.contains(token.owner)) throw NoPermission()

                call.respond(
                    ReportHandler.asReportRequest(ReportHandler.getReportsForFeed(feed.id))
                )
            }

            /** Delete a report in a feed. */
            delete("/{id}") {
                val token = call.isAuthenticated()

                val feedId = call.parameters["feed"] ?: throw InvalidArguments("p_feed")

                val feed = FeedManager.getFeed(feedId)

                // they must be moderator to delete reports.
                if (!feed.moderators.contains(token.owner)) throw NoPermission()

                val id = call.parameters["id"]?.toLongOrNull() ?: throw InvalidArguments("id")

                // get the report and make sure it's from the same feed
                val report = ReportHandler.getReportById(id)

                if (report.feed != feed.id) throw NoPermission()

                ReportHandler.deleteReport(id)

                call.respond(Response("OK"))
            }
        }

        /** Get all reports intended for Unifey. */
        get {
            val token = call.isAuthenticated()

            if (GlobalRoles.STAFF > token.getOwner().role) throw NoPermission()

            call.respond(ReportHandler.asReportRequest(ReportHandler.getReportsForUnifey()))
        }

        /** Delete a report intended for Unifey */
        delete("/{id}") {
            val token = call.isAuthenticated()

            if (GlobalRoles.STAFF > token.getOwner().role) throw NoPermission()

            val id = call.parameters["id"]?.toLongOrNull() ?: throw InvalidArguments("id")

            val report = ReportHandler.getReportById(id)

            if (report.feed != null) throw NoPermission()

            ReportHandler.deleteReport(id)

            call.respond(Response("OK"))
        }
    }
}
