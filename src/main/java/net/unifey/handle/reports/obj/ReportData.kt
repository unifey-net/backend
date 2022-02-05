package net.unifey.handle.reports.obj

import kotlinx.serialization.Serializable

@Serializable
data class ReportData(val url: String, val reportee: String, val target: String)
