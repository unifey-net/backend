package net.unifey.handle.mongo

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClientFactory
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import net.unifey.config.Config
import net.unifey.unifey
import java.lang.Exception

object Mongo {
    private var client: MongoClient? = null

    private fun makeClient() {
        val password = unifey.getConfigObject<Config>().mongoPass

        client = MongoClients.create("mongodb+srv://unify-mongo:${password}@unifey.mahkb.mongodb.net/unifey?retryWrites=true&w=majority")
    }

    fun getClient(): MongoClient {
        if (client == null)
            makeClient()

        return client ?: throw Exception("Failed to load Mongo Client")
    }
}