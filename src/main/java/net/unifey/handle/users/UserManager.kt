package net.unifey.handle.users

import net.unifey.auth.tokens.TokenManager
import net.unifey.auth.tokens.TokenManager.deleteToken
import net.unifey.handle.InvalidVariableInput
import net.unifey.handle.NotFound
import net.unifey.handle.feeds.FeedManager
import net.unifey.handle.live.Live
import net.unifey.handle.mongo.MONGO
import net.unifey.handle.notification.NotificationManager
import net.unifey.handle.users.email.UserEmailManager
import net.unifey.handle.users.member.Member
import net.unifey.handle.users.profile.Profile
import net.unifey.util.IdGenerator
import org.json.JSONObject
import org.litote.kmongo.eq
import org.litote.kmongo.setValue
import org.mindrot.jbcrypt.BCrypt

/** Manage [User]s. */
object UserManager {
    /** Get a user by their [username], then return their ID. */
    suspend fun getId(username: String, ignoreCase: Boolean = true): Long =
        getUser(username, ignoreCase).id

    /** Get a user by their [id]. */
    @Throws(NotFound::class)
    suspend fun getUser(id: Long): User {
        return MONGO.getDatabase("users").getCollection<User>("users").find(User::id eq id).first()
            ?: throw NotFound("user")
    }

    /** Get a user by their [username]. */
    @Throws(NotFound::class)
    suspend fun getUser(username: String, ignoreCase: Boolean = true): User {
        return MONGO
            .getDatabase("users")
            .getCollection<User>("users")
            .find()
            .toList()
            .firstOrNull { user -> user.username.equals(username, ignoreCase) }
            ?: throw NotFound("user")
    }

    /** Generate a unique identifier for a user. */
    suspend fun generateIdentifier(): Long {
        return IdGenerator.getSuspensefulId { id ->
            MONGO.getDatabase("users").getCollection<User>("users").find(User::id eq id).first() !=
                null
        }
    }

    /**
     * Create an account with [email], [username] and [password]. If the account was created through
     * a third party service that would've already verified the email, then [verified] is set to
     * true. ex: google
     */
    @Throws(InvalidVariableInput::class)
    suspend fun createUser(
        email: String,
        username: String,
        password: String,
        verified: Boolean = false
    ): User {
        UserInputRequirements.allMeets(username, password, email)

        val id = generateIdentifier()
        val user =
            User(
                id = id,
                username = username,
                password = BCrypt.hashpw(password, BCrypt.gensalt()),
                email = email,
                verified = verified,
                role = GlobalRoles.DEFAULT,
                createdAt = System.currentTimeMillis()
            )

        val profile = Profile(id, "", "", "", listOf())
        val member = Member(id, listOf(), listOf())
        val db = MONGO.getDatabase("users")

        db.getCollection<Member>("members").insertOne(member)
        db.getCollection<Profile>("profiles").insertOne(profile)
        db.getCollection<User>("users").insertOne(user)

        FeedManager.createFeedForUser(id)

        if (!verified) {
            UserEmailManager.sendVerify(id, email)
            NotificationManager.postNotification(
                id,
                "A verification link has been sent to your email. If you can't see it, visit your settings to resend."
            )
        }

        return getUser(id)
    }

    /** Change a user's [email] by their [id]. */
    suspend fun changeEmail(id: Long, email: String) {
        MONGO
            .getDatabase("users")
            .getCollection<User>("users")
            .updateOne(User::id eq id, setValue(User::email, email))
    }

    /** Change a user's [password] by their [id]. */
    suspend fun changePassword(id: Long, password: String) {
        MONGO
            .getDatabase("users")
            .getCollection<User>("users")
            .updateOne(User::id eq id, setValue(User::password, password))
    }

    /** Change a user's [name] by their [id]. */
    suspend fun changeUsername(id: Long, name: String) {
        MONGO
            .getDatabase("users")
            .getCollection<User>("users")
            .updateOne(User::id eq id, setValue(User::username, name))
    }

    /** Change [id]'s verification status. */
    suspend fun setVerified(id: Long, verified: Boolean) {
        MONGO
            .getDatabase("users")
            .getCollection<User>("users")
            .updateOne(User::id eq id, setValue(User::verified, verified))
    }

    /** Signs out all users connected to the account (aka delete all tokens) */
    suspend fun signOutAll(user: User) {
        TokenManager.getTokensForUser(user).forEach { token -> deleteToken(token) }

        Live.sendUpdate(Live.LiveObject("SIGN_OUT", user.id, JSONObject()))
    }
}
