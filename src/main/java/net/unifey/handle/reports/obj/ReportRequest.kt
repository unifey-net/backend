package net.unifey.handle.reports.obj

import kotlinx.serialization.Serializable

@Serializable
data class ReportRequest(val report: Report, val data: ReportData)
