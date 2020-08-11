package net.unifey.handle.communities.rules

import com.fasterxml.jackson.annotation.JsonIgnore
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import net.unifey.handle.mongo.Mongo
import kotlin.properties.Delegates
import kotlin.properties.Delegates.observable

/**
 * One of a communities' rules.
 */
data class CommunityRule(
        @JsonIgnore
        private val id: Long,
        var index: Int,
        var title: String,
        var body: String
)