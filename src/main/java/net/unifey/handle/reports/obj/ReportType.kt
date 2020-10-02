package net.unifey.handle.reports.obj

import net.unifey.handle.reports.TargetType

enum class ReportType(vararg val types: TargetType) {
    HACKED(TargetType.ACCOUNT),

    DOES_NOT_FIT_TOPIC(TargetType.COMMENT, TargetType.POST),

    SPAM(TargetType.COMMENT, TargetType.POST, TargetType.ACCOUNT)
}