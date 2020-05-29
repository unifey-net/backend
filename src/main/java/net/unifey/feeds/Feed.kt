package net.unifey.feeds

import org.json.JSONArray

data class Feed(
        val id: String,
        val banned: JSONArray,
        val moderators: JSONArray
)