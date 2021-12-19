package net.unifey.handle.mongo

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import kotlinx.coroutines.*
import net.unifey.mongo
import net.unifey.prod
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

/** Interacts with MongoDB. */
object Mongo {
    val K_MONGO =
        KMongo.createClient(
                if (prod) {
                    "mongodb+srv://unify-mongo:${mongo}@unifey.mahkb.mongodb.net/unifey?retryWrites=true&w=majority"
                } else {
                    "mongodb://127.0.0.1:27017"
                })
            .coroutine

    /** The MongoClient. */
    private var client: MongoClient? = null

    /**
     * Create a MongoDB client, either with a local DB or production depending on [prod].
     *
     * This sets [client].
     */
    private fun makeClient() {
        client =
            if (prod) {
                MongoClients.create(
                    "mongodb+srv://unify-mongo:${mongo}@unifey.mahkb.mongodb.net/unifey?retryWrites=true&w=majority")
            } else {
                MongoClients.create("mongodb://127.0.0.1:27017") // local testing mongodb server
            }
    }

    suspend fun <T> useJob(func: suspend MongoClient.() -> T): Job {
        return coroutineScope { launch { func.invoke(getClient()) } }
    }

    suspend fun <T> useAsync(func: suspend MongoClient.() -> T): Deferred<T> {
        return coroutineScope { async(Dispatchers.Default) { func.invoke(getClient()) } }
    }

    /** Get [client] and assure it's not null. */
    fun getClient(): MongoClient {
        if (client == null) makeClient()

        return client ?: throw Exception("Failed to load Mongo Client")
    }
}
