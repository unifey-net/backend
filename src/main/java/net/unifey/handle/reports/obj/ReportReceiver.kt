package net.unifey.handle.reports.obj

/**
 * A report receiver.
 *
 * @param type 0 = Unifey staff members, 1 = community moderators
 * @param id The ID of the community, if it's community.
 */
data class ReportReceiver(
        val type: Int,
        val id: Long
)