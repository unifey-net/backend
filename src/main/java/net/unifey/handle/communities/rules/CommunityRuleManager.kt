package net.unifey.handle.communities.rules

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import net.unifey.handle.communities.CommunityManager
import net.unifey.handle.mongo.Mongo
import net.unifey.util.IdGenerator
import org.bson.Document

object CommunityRuleManager {
    /**
     * Create a rule for [community].
     * The rules index is the first one available.
     *
     * @param title The title of the rule.
     * @param body The body of the rule.
     * @param community The communities' id.
     */
    suspend fun createRule(title: String, body: String, community: Long) {
        Mongo.useJob {
            getDatabase("communities")
                    .getCollection("rules")
                    .insertOne(Document(mapOf(
                            "id" to community,
                            "index" to getRules(community).size,
                            "title" to title,
                            "body" to body
                    )))
        }
    }

    /**
     * Modify a rule's body.
     *
     * @param body The new body for the rule.
     * @param index The index of the rule.
     * @param community The community where the rule resides.
     */
    suspend fun modifyBody(body: String, index: Int, community: Long) {
        Mongo.useJob {
            getDatabase("communities")
                    .getCollection("rules")
                    .updateOne(
                            Filters.and(Filters.eq("id", community), Filters.eq("index", index)),
                            Updates.set("body", body)
                    )
        }
    }

    /**
     * Modify a rule's title.
     *
     * @param title The new title for the rule.
     * @param index The index of the rule.
     * @param community The community where the rule resides.
     */
    suspend fun modifyTitle(title: String, index: Int, community: Long) {
        Mongo.useJob {
            getDatabase("communities")
                    .getCollection("rules")
                    .updateOne(
                            Filters.and(Filters.eq("id", community), Filters.eq("index", index)),
                            Updates.set("title", title)
                    )
        }
    }

    /**
     * Modify a rule's index.
     *
     * @param newIndex The new index for the rule.
     * @param oldIndex The current index of the rule.
     * @param community The community where the rule resides.
     */
    suspend fun modifyIndex(newIndex: Int, oldIndex: Int, community: Long) {
        val rules = getRules(community)
    }

    /**
     * Delete a rule by it's index.
     *
     * @param index The index of the rule
     * @param community The community where the rule resides.
     */
    suspend fun deleteRule(index: Int, community: Long) {
        Mongo.useJob {
            getDatabase("communities")
                    .getCollection("rules")
                    .deleteOne(Filters.and(Filters.eq("id", community), Filters.eq("index", index)))
        }
    }

    /**
     * Delete all rules from [community].
     */
    suspend fun deleteAll(community: Long) {
        Mongo.useJob {
            getDatabase("communities")
                    .getCollection("rules")
                    .deleteMany(Filters.eq("id", community))
        }
    }

    /**
     * Get [community]'s rules.
     */
    suspend fun getRules(community: Long): MutableList<CommunityRule> {
        return Mongo.useAsync {
            getDatabase("communities")
                    .getCollection("rules")
                    .find(Filters.eq("id", community))
                    .map { doc ->
                        CommunityRule(
                                community,
                                doc.getInteger("index"),
                                doc.getString("title"),
                                doc.getString("body")
                        )
                    }
                    .toMutableList()
        }.await()
    }
}