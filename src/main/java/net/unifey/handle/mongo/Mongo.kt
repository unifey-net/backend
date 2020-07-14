package net.unifey.handle.mongo

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClientFactory
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import net.unifey.auth.tokens.TokenManager
import net.unifey.config.Config
import net.unifey.handle.users.UserManager
import net.unifey.prod
import net.unifey.unifey
import java.lang.Exception

object Mongo {
    private var client: MongoClient? = null

    private fun makeClient() {
        client = if (prod) {
            val password = unifey.getConfigObject<Config>().mongoPass

            MongoClients.create("mongodb+srv://unify-mongo:${password}@unifey.mahkb.mongodb.net/unifey?retryWrites=true&w=majority")
        } else {
            MongoClients.create("mongodb://127.0.0.1:27017") // local testing mongodb server
        }
    }

    fun getClient(): MongoClient {
        if (client == null)
            makeClient()

        return client ?: throw Exception("Failed to load Mongo Client")
    }
}