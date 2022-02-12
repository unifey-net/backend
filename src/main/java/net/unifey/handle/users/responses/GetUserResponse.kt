package net.unifey.handle.users.responses

import kotlinx.serialization.Serializable
import net.unifey.handle.users.User
import net.unifey.handle.users.member.Member
import net.unifey.handle.users.member.MemberManager
import net.unifey.handle.users.profile.Profile
import net.unifey.handle.users.profile.ProfileManager

/** The response for a [User] */
@Serializable
data class GetUserResponse(val user: User, val member: Member?, val profile: Profile) {
    companion object {
        /**
         * Get a [GetUserResponse] from a [User].
         *
         * @param isSelf If true, [Member] will be included.
         */
        suspend fun User.response(isSelf: Boolean = false): GetUserResponse {
            return GetUserResponse(
                this,
                if (isSelf) MemberManager.getMember(id) else null,
                ProfileManager.getProfile(id)
            )
        }
    }
}
