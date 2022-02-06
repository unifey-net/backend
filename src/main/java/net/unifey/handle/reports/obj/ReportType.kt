package net.unifey.handle.reports.obj

import net.unifey.handle.reports.TargetType

/** Where the report should go to. */
enum class ReportType(vararg val types: TargetType) {
    /** Report a comment or post to the community moderators. */
    COMMUNITY(TargetType.COMMENT, TargetType.POST),

    /** Report a command, post or account to Unifey. */
    UNIFEY(TargetType.COMMENT, TargetType.POST, TargetType.ACCOUNT)
}
