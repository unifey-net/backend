package net.unifey.handle.users.member

import com.fasterxml.jackson.annotation.JsonIgnore
import kotlinx.serialization.Serializable

/**
 * @param id The user's ID.
 * @param member The communities the user's in.
 * @param notifications The communities the user has notifications on for.
 */
@Serializable
data class Member(val id: Long, val member: List<Long>, val notifications: List<Long>)
