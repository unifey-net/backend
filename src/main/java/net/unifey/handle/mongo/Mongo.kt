package net.unifey.handle.mongo

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClients
import com.mongodb.reactivestreams.client.MongoClient
import kotlinx.coroutines.*
import net.unifey.Unifey
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

val MONGO
    get() = Mongo.K_MONGO

/** Interacts with MongoDB. */
object Mongo {
    val K_MONGO =
        KMongo.createClient(
                MongoClientSettings.builder()
                    .applyConnectionString(
                        ConnectionString(
                            if (Unifey.prod) {
                                Unifey.mongo
                            } else {
                                "mongodb://127.0.0.1:27017"
                            }
                        )
                    )
                    .retryWrites(false)
                    .build()
            )
            .coroutine

    /** The MongoClient. */
    private var client: com.mongodb.client.MongoClient? = null

    /**
     * Create a MongoDB client, either with a local DB or production depending on [prod].
     *
     * This sets [client].
     */
    private fun makeClient() {
        client =
            if (Unifey.prod) {
                MongoClients.create(Unifey.mongo)
            } else {
                MongoClients.create("mongodb://127.0.0.1:27017") // local testing mongodb server
            }
    }

    suspend fun <T> useJob(func: suspend com.mongodb.client.MongoClient.() -> T): Job {
        return coroutineScope { launch { func.invoke(getClient()) } }
    }

    suspend fun <T> useAsync(func: suspend com.mongodb.client.MongoClient.() -> T): Deferred<T> {
        return coroutineScope { async(Dispatchers.Default) { func.invoke(getClient()) } }
    }

    /** Get [client] and assure it's not null. */
    fun getClient(): com.mongodb.client.MongoClient {
        if (client == null) makeClient()

        return client ?: throw Exception("Failed to load Mongo Client")
    }
}
