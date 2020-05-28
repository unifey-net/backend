package net.unifey

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.sql.Connection
import java.sql.DriverManager

object DatabaseHandler {
    private val URL: String = "jdbc:sql://shog-dev.clygxxgfxolj.us-east-2.rds.amazonaws.com:5432/unifey"
    private val USERNAME: String = "unifey"
    private val PASSWORD: String = "unifey"

    /**
     * Create a connection to AWS.
     */
    fun createConnection(): Connection {
        Class.forName("org.mysql.Driver")
        return DriverManager.getConnection(URL, USERNAME, PASSWORD)
    }
}