package net.unifey.util

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import io.ktor.routing.*
import kotlin.jvm.Throws
import net.unifey.handle.mongo.Mongo
import org.bson.Document

object FieldChangeLimiter {
    /**
     * Check if an object's field is limited. If it is, return when it's no longer limited.
     *
     * @param type Either `COMMUNITY` or `USER`.
     * @param ID The ID of the community or user.
     * @param field The field that's being limited. For example, a user can only change their
     * `USERNAME` once a month.
     */
    fun isLimited(type: String, id: Long, field: String): Pair<Boolean, Long?> {
        val find =
            Mongo.getClient()
                .getDatabase("global")
                .getCollection("namechange")
                .find(
                    Filters.and(
                        Filters.eq("id", id), Filters.eq("type", type), Filters.eq("field", field)))
                .firstOrNull()

        if (find != null) {
            val time = find.getLong("until")

            return if (System.currentTimeMillis() >= time) {
                delete(type, id, field)
                false to -1
            } else {
                true to time
            }
        }

        return false to -1
    }

    /**
     * Create a limit for [name].
     *
     * @param type Either `COMMUNITY` or `USER`.
     * @param id The ID of the community or user.
     * @param field The field that's being limited.
     */
    fun createLimit(type: String, id: Long, field: String, time: Long) {
        if (isLimited(type, id, field).first) {
            Mongo.getClient()
                .getDatabase("global")
                .getCollection("namechange")
                .updateOne(
                    Filters.and(
                        Filters.eq("id", id), Filters.eq("type", type), Filters.eq("field", field)),
                    Updates.set("until", time))
        } else {
            Mongo.getClient()
                .getDatabase("global")
                .getCollection("namechange")
                .insertOne(
                    Document(mapOf("id" to id, "type" to type, "field" to field, "until" to time)))
        }
    }

    /** Delete by [type], [name], and [field]. */
    fun delete(type: String, id: Long, field: String) {
        Mongo.getClient()
            .getDatabase("global")
            .getCollection("namechange")
            .deleteOne(
                Filters.and(
                    Filters.eq("type", type), Filters.eq("id", id), Filters.eq("field", field)))
    }

    /**
     * Inline check if a [field] is ratelimit for [type] + [id]. throws [RateLimitException] if
     * limited
     */
    @Throws(RateLimitException::class)
    fun checkLimited(type: String, id: Long, field: String) {
        val (limit, time) = isLimited(type, id, field)

        if (limit && time != null) throw RateLimitException(time - System.currentTimeMillis(), time)
    }
}
