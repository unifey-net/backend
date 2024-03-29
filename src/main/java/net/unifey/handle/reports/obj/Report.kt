package net.unifey.handle.reports.obj

import kotlinx.serialization.Serializable
import net.unifey.handle.reports.Target

/**
 * A report.
 *
 * @param id The ID of the report.
 * @param feed The feed
 * @param reportee The person reporting.
 * @param target The target object.
 * @param reportType If it's either Unifey or community moderators.
 * @param reason The user extended reason (this can be blank)
 * @param date The date the report occurred
 */
@Serializable
data class Report(
    val id: Long,
    val feed: String?,
    val reportee: Long,
    val target: Target,
    val reportType: ReportType,
    val reason: String,
    val date: Long
)
