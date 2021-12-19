package net.unifey.handle.users.connections

import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.unifey.handle.HTTP_CLIENT
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.connections.handlers.ConnectionHandler
import net.unifey.handle.users.connections.handlers.Google
import org.json.JSONObject
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.reactivestreams.KMongo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.system.measureTimeMillis

object ConnectionManager {
    val LOGGER: Logger = LoggerFactory.getLogger(this.javaClass)

    enum class Type(val handler: ConnectionHandler) {
        GOOGLE(Google)
    }

    /**
     * A connection between an account and another service.
     */
    data class Connection(val user: Long, val type: Type, val serviceId: String)

    /**
     * Find a [Connection] by it's [type] and [serviceId].
     *
     * @returns A connection, if found. Otherwise null;
     */
    suspend fun findConnection(type: Type, serviceId: String): Connection? {
        var search: Connection?

        val time = measureTimeMillis {
            search = Mongo.K_MONGO
                .getDatabase("users")
                .getCollection<Connection>("connections")
                .find(and(Connection::serviceId eq serviceId, Connection::type eq type))
                .first()
        }

        LOGGER.trace("Search Connection: $type -> $serviceId (service id); Took ${time}ms")

        return search
    }

    /**
     * Find a connection by it's [type] and a user's [id].
     *
     * @returns A connection, if found. Otherwise null;
     */
    suspend fun findConnection(type: Type, id: Long): Connection? {
        var search: Connection?

        val time = measureTimeMillis {
            search = Mongo.K_MONGO
                .getDatabase("users")
                .getCollection<Connection>("connections")
                .find(and(Connection::user eq id, Connection::type eq type))
                .first()
        }

        LOGGER.trace("Search Connection: $type -> $id (user); Took ${time}ms")

        return search
    }

    /**
     * Create a connection for [id] with [type] to [serviceId]
     */
    suspend fun createConnection(type: Type, id: Long, serviceId: String) {
        LOGGER.trace("New Connection: $serviceId ($type) -> $id")

        Mongo.K_MONGO
            .getDatabase("users")
            .getCollection<Connection>("connections")
            .insertOne(Connection(id, type, serviceId))
    }
}