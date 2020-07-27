package net.unifey.handle.reports

data class Report(
        val reportee: Long,
        val target: Long,
        val targetType: ReportHandler.TargetType,
        val reason: String,
        val date: Long
)