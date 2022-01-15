package net.unifey.handle.users.responses

import kotlinx.serialization.Serializable
import net.unifey.handle.users.User
import net.unifey.handle.users.member.Member
import net.unifey.handle.users.member.MemberManager
import net.unifey.handle.users.profile.Profile
import net.unifey.handle.users.profile.ProfileManager

/** The response for a [User] */
@Serializable
data class GetUserResponse(val user: User, val member: Member, val profile: Profile) {
    companion object {
        /** Get a [GetUserResponse] from a [User]. */
        suspend fun User.response(): GetUserResponse {
            return GetUserResponse(this, MemberManager.getMember(id), ProfileManager.getProfile(id))
        }
    }
}
