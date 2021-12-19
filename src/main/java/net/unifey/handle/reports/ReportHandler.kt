package net.unifey.handle.reports

import com.mongodb.client.model.Filters
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import java.util.concurrent.TimeUnit
import net.unifey.handle.Error
import net.unifey.handle.NotFound
import net.unifey.handle.communities.CommunityManager
import net.unifey.handle.feeds.posts.PostManager
import net.unifey.handle.feeds.posts.comments.CommentManager
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.reports.obj.Report
import net.unifey.handle.reports.obj.ReportData
import net.unifey.handle.reports.obj.ReportRequest
import net.unifey.handle.reports.obj.ReportType
import net.unifey.handle.users.UserManager
import net.unifey.response.Response
import net.unifey.util.IdGenerator
import org.bson.Document

object ReportHandler {
    const val MAX_REPORT_PER_PERSON = 3

    /** Delete a report by it's [id]. */
    suspend fun deleteReport(id: Long) {
        reportCache.removeIf { report -> report.id == id }

        Mongo.useJob {
            getDatabase("global").getCollection("reports").deleteOne(Filters.eq("id", id))
        }
    }

    /** Get a report by it's [id] */
    fun getReportById(id: Long): Report {
        return reportCache.singleOrNull { report -> report.id == id } ?: throw NotFound("report")
    }

    /** The report cache. */
    private val reportCache: MutableList<Report> by lazy {
        fun getTarget(document: Document): Target {
            return Target(document.getLong("id"), TargetType.valueOf(document.getString("type")))
        }

        Mongo.getClient()
            .getDatabase("global")
            .getCollection("reports")
            .find()
            .map { doc ->
                Report(
                    doc.getLong("id"),
                    doc.getString("feed") ?: null,
                    doc.getLong("reportee"),
                    getTarget(doc.get("target", Document::class.java)),
                    ReportType.valueOf(doc.getString("type")),
                    doc.getString("reason"),
                    doc.getLong("date"))
            }
            .toMutableList()
    }

    /** Get reports for a feed. */
    fun getReportsForFeed(feed: String): List<Report> {
        return reportCache.filter { report -> report.feed == feed }.filter { report ->
            report.reportType == ReportType.COMMUNITY
        }
    }

    /** Get reports for Unifey. */
    fun getReportsForUnifey(): List<Report> {
        return reportCache.filter { report ->
            report.reportType == ReportType.UNIFEY || report.feed == null
        }
    }

    /** Get all reports for [target]. */
    fun getReportsForTarget(target: Target, feed: String?): List<Report> {
        return reportCache.filter { report -> report.target == target }.filter { report ->
            report.feed == feed
        }
    }

    /** Get reports today from [reportee]. Used to make sure people don't spam reports. */
    fun getReportsToday(reportee: Long): Int {
        return reportCache
            .filter { report -> report.reportee == reportee }
            .filter { report ->
                System.currentTimeMillis() - report.date < TimeUnit.DAYS.toMillis(1)
            }
            .size
    }

    /** Add a report for [target]. */
    fun addReport(target: Target, type: ReportType, feed: String?, reportee: Long, reason: String) {
        if (getReportsToday(reportee) > MAX_REPORT_PER_PERSON)
            throw Error({
                respond(HttpStatusCode.BadRequest, Response("You can only report 3 times per day!"))
            })

        val id =
            IdGenerator.getId {
                Mongo.getClient()
                    .getDatabase("global")
                    .getCollection("reports")
                    .find(Filters.eq("id", it))
                    .any()
            }

        val report = Report(id, feed, reportee, target, type, reason, System.currentTimeMillis())

        Mongo.getClient()
            .getDatabase("global")
            .getCollection("reports")
            .insertOne(
                Document(
                    mapOf(
                        "id" to id,
                        "reportee" to report.reportee,
                        "target" to
                            Document(
                                mapOf(
                                    "id" to report.target.id,
                                    "type" to report.target.type.toString())),
                        "reason" to report.reason,
                        "date" to report.date,
                        "feed" to report.feed,
                        "type" to report.reportType.toString())))

        reportCache.add(report)
    }

    /** Format [Report]s into [ReportRequest] */
    suspend fun asReportRequest(reports: List<Report>): List<ReportRequest> {
        /** Get if it's a community or user feed & get the name of the id (forms the /u/SHO url) */
        suspend fun getFeedData(feed: String): Pair<String, String> {
            return if (feed.startsWith("cf_")) {
                val id = feed.removePrefix("cf_").toLong()

                "c" to CommunityManager.getCommunityById(id).name
            } else {
                val id = feed.removePrefix("uf_").toLong()

                "u" to UserManager.getUser(id).username
            }
        }

        /**
         * Get the URL of the post/comment/account & get the target user's username (post author
         * etc)
         */
        suspend fun getUrlAndTarget(report: Report): Pair<String, String> {
            when (report.target.type) {
                TargetType.POST -> {
                    val post = PostManager.getPost(report.target.id)

                    val name = getFeedData(post.feed)

                    return UserManager.getUser(post.authorId).username to
                        "/${name.first}/${name.second}/${post.id}"
                }
                TargetType.COMMENT -> {
                    val comment = CommentManager.getCommentById(report.target.id)

                    val name = getFeedData(comment.feed)

                    return UserManager.getUser(comment.authorId).username to
                        "/${name.first}/${name.second}/${comment.post}?comment=${comment.id}"
                }
                TargetType.ACCOUNT -> {
                    val user = UserManager.getUser(report.target.id)

                    return user.username to "/${user.username}"
                }
            }
        }

        suspend fun getReportee(report: Report): String {
            return UserManager.getUser(report.reportee).username
        }

        return reports.map { report ->
            val (target, url) = getUrlAndTarget(report)

            ReportRequest(report, ReportData(url, getReportee(report), target))
        }
    }
}
