package net.unifey.handle.communities.rules

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import net.unifey.handle.Error
import net.unifey.handle.communities.Community
import net.unifey.handle.communities.CommunityManager
import net.unifey.handle.mongo.Mongo
import net.unifey.response.Response
import net.unifey.util.IdGenerator
import org.bson.Document
import kotlin.math.sign

object CommunityRuleManager {
    private const val MAX_RULES = 32

    /**
     * Create a rule for [community].
     * The rules index is the first one available.
     *
     * @param title The title of the rule.
     * @param body The body of the rule.
     * @param community The community.
     */
    @Throws(Error::class)
    suspend fun createRule(title: String, body: String, community: Community) {
        if (community.rules.size >= MAX_RULES)
            throw Error {
                respond(HttpStatusCode.BadRequest, Response("You cannot have over 32 rules!"))
            }

        val id = IdGenerator.getId()

        Mongo.useJob {
            getDatabase("communities")
                    .getCollection("communities")
                    .updateOne(
                            Filters.eq("id", community.id),
                            Updates.set("rules.${id}",
                                    Document(mapOf(
                                            "title" to title,
                                            "body" to body
                                    ))))
        }

        community.rules.add(CommunityRule(
                id,
                title,
                body
        ))
    }

    /**
     * Modify a rule's title.
     *
     * @param title The new title for the rule.
     * @param id The ID of the rule.
     * @param community The community where the rule resides.
     */
    suspend fun modifyTitle(title: String, id: Long, community: Community) {
        Mongo.useJob {
            getDatabase("communities")
                    .getCollection("communities")
                    .updateOne(
                            Filters.eq("id", community.id),
                            Updates.set("rules.${id}.title", title)
                    )
        }

        community.rules
                .single { rule -> rule.id == id }
                .title = title
    }

    /**
     * Modify a rule's body.
     *
     * @param body The new body for the rule.
     * @param id The ID of the rule.
     * @param community The community where the rule resides.
     */
    suspend fun modifyBody(body: String, id: Long, community: Community) {
        Mongo.useJob {
            getDatabase("communities")
                    .getCollection("communities")
                    .updateOne(
                            Filters.eq("id", community.id),
                            Updates.set("rules.${id}.body", body)
                    )
        }

        community.rules
                .single { rule -> rule.id == id }
                .body = body
    }

    /**
     * Delete a rule by it's ID.
     *
     * @param id The ID of the rule.
     * @param community The community where the rule resides.
     */
    suspend fun deleteRule(id: Long, community: Community) {
        Mongo.useJob {
            getDatabase("communities")
                    .getCollection("communities")
                    .updateOne(Filters.eq("id", community.id), Updates.unset("rules.${id}"))
        }

        community.rules.removeIf { rule -> rule.id == id }
    }
}