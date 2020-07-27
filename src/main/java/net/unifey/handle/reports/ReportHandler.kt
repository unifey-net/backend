package net.unifey.handle.reports

import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import net.unifey.handle.Error
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.email.TooManyAttempts
import net.unifey.response.Response
import org.bson.Document
import java.util.concurrent.TimeUnit

object ReportHandler {
    enum class TargetType {
        POST, COMMENT, ACCOUNT
    }

    /**
     * The report cache.
     */
    private val reportCache: MutableList<Report> by lazy {
        Mongo.getClient()
                .getDatabase("global")
                .getCollection("reports")
                .find()
                .map { doc ->
                    Report(
                            doc.getLong("reportee"),
                            doc.getLong("target"),
                            TargetType.valueOf(doc.getString("targetType")),
                            doc.getString("reason"),
                            doc.getLong("date")
                    )
                }
                .toMutableList()
    }

    /**
     * Get all reports for [target] and [targetType]
     */
    fun getReportsForTarget(target: Long, targetType: TargetType): List<Report> {
        return reportCache
                .filter { report -> report.target == target }
                .filter { report -> report.targetType == targetType }
    }

    /**
     * Get reports today from [reportee]. Used to make sure people don't spam reports.
     */
    private fun getReportsToday(reportee: Long): Int {
        return reportCache
                .filter { report -> report.reportee == reportee }
                .filter { report -> System.currentTimeMillis() - report.date < TimeUnit.DAYS.toMillis(1) }
                .size
    }

    /**
     * Add a report for [target].
     */
    fun addReport(target: Long, targetType: TargetType, reportee: Long, reason: String) {
        if (getReportsToday(reportee) > 3)
            throw Error {
                respond(HttpStatusCode.BadRequest, Response("You can only report 3 times per day!"))
            }

        val report = Report(
                reportee,
                target,
                targetType,
                reason,
                System.currentTimeMillis()
        )

        Mongo.getClient()
                .getDatabase("global")
                .getCollection("reports")
                .insertOne(Document(mapOf(
                        "reportee" to report.reportee,
                        "target" to report.target,
                        "targetType" to report.targetType.toString(),
                        "reason" to report.reason,
                        "date" to report.date
                )))

        reportCache.add(report)
    }
}