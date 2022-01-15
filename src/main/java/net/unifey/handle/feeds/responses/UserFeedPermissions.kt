package net.unifey.handle.feeds.responses

import kotlinx.serialization.Serializable

@Serializable
data class UserFeedPermissions(val canView: Boolean, val canPost: Boolean, val canComment: Boolean)
