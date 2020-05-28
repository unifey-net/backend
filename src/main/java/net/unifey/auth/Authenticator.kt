package net.unifey.auth

import net.unifey.DatabaseHandler
import org.apache.commons.codec.digest.DigestUtils
import kotlin.streams.asSequence

object Authenticator {
    fun createAccount(email: String, username: String, password: String): Boolean {
        if (emailInUse(email))
            return false
        if (usernameTaken(username))
            return false;

        var connection = DatabaseHandler.createConnection()
        var stmt = connection.prepareStatement("INSERT INTO users (email, username, password) VALUES (?, ?, ?)")
        stmt.setString(1, email)
        stmt.setString(2, username)
        val salt = generateRandomString(8)
        val hashedPassword = DigestUtils.sha256Hex(password + salt)
        val finalPassword = "$salt:$hashedPassword"
        stmt.setString(3, finalPassword)
        stmt.execute()

        return true;
    }

    fun usernameTaken(username: String): Boolean {
        var connection = DatabaseHandler.createConnection()
        var stmt = connection.prepareStatement("SELECT * FROM users WHERE username = ?")
        stmt.setString(1, username)
        stmt.execute()
        if (stmt.resultSet.fetchSize > 0)
            return true
        return false
    }

    fun emailInUse(email: String): Boolean {
        var connection = DatabaseHandler.createConnection()
        var stmt = connection.prepareStatement("SELECT * FROM users WHERE email = ?")
        stmt.setString(1, email)
        stmt.execute()
        if (stmt.resultSet.fetchSize > 0)
            return true
        return false
    }

    fun generateIfCorrect(username: String, password: String): String {
        var connection = DatabaseHandler.createConnection()
        var stmt = connection.prepareStatement("SELECT * FROM users WHERE username = ?")
        stmt.setString(1, username)
        stmt.execute()

        var databasePassword = stmt.resultSet.getString("password")
        var chunks = databasePassword.split(":");
        var salt = chunks[0]
        var hash = chunks[1]

        if (DigestUtils.sha256Hex(password + salt) == hash)
            return generateToken()

        return "N/A"
    }

    private fun generateToken(): String {
        val source = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val before = generateRandomString(32)
        return DigestUtils.sha256Hex(before)
    }

    private fun generateRandomString(len: Long): String {
        val source = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return java.util.Random().ints(len, 0, source.length).asSequence().map(source::get).joinToString("")
    }
}