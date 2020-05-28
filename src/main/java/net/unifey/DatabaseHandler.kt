package net.unifey

import com.mysql.cj.jdbc.MysqlDataSource
import net.unifey.config.Config
import java.sql.Connection

/**
 * Connect to the database.
 */
object DatabaseHandler {
    private val url: String
    private val username: String
    private val password: String

    init {
        val obj = unifeyCfg.asObject<Config>()

        url = obj.url ?: ""
        password = obj.password ?: ""
        username = obj.username ?: ""
    }

    /**
     * Create a connection to AWS.
     */
    fun createConnection(): Connection {
        val source = MysqlDataSource()

        source.user = username
        source.password = password
        source.serverName = url
        source.databaseName = "unifey"
        source.serverTimezone = "CST"

        return source.connection
    }
}