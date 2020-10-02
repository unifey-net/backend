package net.unifey.handle.reports.obj

import net.unifey.handle.reports.Target

/**
 * A report.
 *
 * @param id The ID of the report.
 * @param feed The feed
 * @param reportee The person reporting.
 * @param target The target object.
 * @param reportType The blanket reason for reporting.
 * @param reason The user extended reason (this can be blank)
 * @param date The date the report occurred
 */
data class Report(
        val id: Long,
        val feed: String?,
        val reportee: Long,
        val target: Target,
        val reportType: ReportType,
        val reason: String,
        val date: Long
)