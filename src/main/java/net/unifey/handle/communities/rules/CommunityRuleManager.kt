package net.unifey.handle.communities.rules

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import dev.ajkneisl.lib.util.getOrNull
import io.ktor.http.*
import io.ktor.server.response.respond
import net.unifey.handle.Error
import net.unifey.handle.NotFound
import net.unifey.handle.communities.Community
import net.unifey.handle.mongo.MONGO
import net.unifey.handle.mongo.Mongo
import net.unifey.response.Response
import net.unifey.util.IdGenerator
import org.bouncycastle.asn1.x500.style.RFC4519Style.title
import org.litote.kmongo.*

object CommunityRuleManager {
    private const val MAX_RULES = 32

    /**
     * Create a rule for [community]. The rules index is the first one available.
     *
     * @param title The title of the rule.
     * @param body The body of the rule.
     * @param community The community.
     *
     * @return The new rule's id
     */
    @Throws(Error::class)
    suspend fun createRule(title: String, body: String, community: Community): Long {
        if (community.rules.size >= MAX_RULES)
            throw Error({
                respond(HttpStatusCode.BadRequest, Response("You cannot have over 32 rules!"))
            })

        val id = IdGenerator.getId()

        community.rules[id] = CommunityRule(id, title, body)

        MONGO
            .getDatabase("communities")
            .getCollection<Community>("communities")
            .updateOne(Community::id eq community.id, setValue(Community::rules, community.rules))

        return id
    }

    /**
     * Modify a rule's title.
     *
     * @param title The new title for the rule.
     * @param id The ID of the rule.
     * @param community The community where the rule resides.
     */
    @Throws(NotFound::class)
    suspend fun modifyTitle(title: String, id: Long, community: Community) {
        val rule = community.rules.getOrNull(id) ?: throw NotFound("rule")

        community.rules[id] = CommunityRule(id, title, rule.body)

        MONGO
            .getDatabase("communities")
            .getCollection<Community>("communities")
            .updateOne(Community::id eq community.id, setValue(Community::rules, community.rules))
    }

    /**
     * Modify a rule's body.
     *
     * @param body The new body for the rule.
     * @param id The ID of the rule.
     * @param community The community where the rule resides.
     */
    @Throws(NotFound::class)
    suspend fun modifyBody(body: String, id: Long, community: Community) {
        val rule = community.rules.getOrNull(id) ?: throw NotFound("rule")

        community.rules[id] = CommunityRule(id, rule.title, body)

        MONGO
            .getDatabase("communities")
            .getCollection<Community>("communities")
            .updateOne(Community::id eq community.id, setValue(Community::rules, community.rules))
    }

    /**
     * Delete a rule by it's ID.
     *
     * @param id The ID of the rule.
     * @param community The community where the rule resides.
     */
    suspend fun deleteRule(id: Long, community: Community) {
        community.rules.remove(id)

        MONGO
            .getDatabase("communities")
            .getCollection<Community>("communities")
            .updateOne(Community::id eq community.id, setValue(Community::rules, community.rules))
    }
}
