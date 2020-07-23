package net.unifey.handle.users.email

import com.amazonaws.regions.Regions
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder
import com.amazonaws.services.simpleemail.model.*
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import dev.shog.lib.util.currentTimeMillis
import net.unifey.handle.InvalidArguments
import net.unifey.handle.NoPermission
import net.unifey.handle.NotFound
import net.unifey.handle.beta.Beta
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.UserManager
import net.unifey.handle.users.email.defaults.Email
import net.unifey.unifey
import net.unifey.util.IdGenerator
import org.bson.Document
import org.json.JSONObject
import org.mindrot.jbcrypt.BCrypt
import java.lang.Exception

object UserEmailManager {
    /**
     * The verification requests
     */
    private val verifyRequests: MutableList<UserEmailRequest> by lazy {
        Mongo.getClient()
                .getDatabase("email")
                .getCollection("verify")
                .find()
                .map { doc ->
                    UserEmailRequest(
                            doc.getLong("id"),
                            doc.getString("email"),
                            doc.getString("verify"),
                            doc.getInteger("type"),
                            doc.getInteger("attempts")
                    )
                }
                .toMutableList()
    }

    /**
     * Get an email request.
     */
    @Throws(NotFound::class)
    fun getRequest(id: Long, type: Int): UserEmailRequest {
        return verifyRequests
                .firstOrNull { request -> request.id == id && request.type == type }
                ?: throw NotFound("emailRequest")
    }

    /**
     * The users who are unsubscribed
     */
    private val unsubscribed: MutableList<Pair<String, Long>> by lazy {
        Mongo.getClient()
                .getDatabase("email")
                .getCollection("unsubscribed")
                .find()
                .map { doc -> doc.getString("email") to doc.getLong("time") }
                .toMutableList()
    }

    /**
     * Verify [id] using [verify]
     */
    @Throws(InvalidArguments::class)
    fun verify(id: Long, verify: String) {
        verifyRequests.singleOrNull { request ->
            request.type == EmailTypes.VERIFY_EMAIL.id
                    && request.id == id
                    && request.verify.equals(verify, true)
        }
                ?: throw InvalidArguments("verify")

        verifyRequests.removeIf { request -> request.verify.equals(verify, true) }

        Mongo.getClient()
                .getDatabase("email")
                .getCollection("verify")
                .deleteOne(Filters.and(eq("id", id), eq("verify", verify)))

        UserManager.getUser(id).verified = true
    }

    /**
     * Verify using [verify]
     */
    @Throws(InvalidArguments::class)
    fun betaVerify(verify: String) {
        val req = verifyRequests.singleOrNull { request ->
            request.type == EmailTypes.VERIFY_BETA.id && request.verify.equals(verify, true)
        }
                ?: throw InvalidArguments("verify")

        verifyRequests.removeIf { request -> request.verify.equals(verify, true) }

        Mongo.getClient()
                .getDatabase("email")
                .getCollection("verify")
                .deleteOne(eq("verify", verify))

        Beta.verify(req.email)
    }

    /**
     * Send a beta verify to [email].
     */
    @Throws(AlreadyVerified::class, Unverified::class)
    fun sendBetaVerify(email: String) {
        if (Beta.isVerified(email))
            throw AlreadyVerified()

        val verify = IdGenerator.generateRandomString(32)

        val exists = verifyRequests.singleOrNull { request ->
            request.type == EmailTypes.VERIFY_BETA.id && request.email.equals(email, true)
        }

        // If there's already an ongoing email request, you can't change it until you've confirmed your first.
        if (exists != null)
            throw NoPermission()

        val doc = Document(mapOf(
                "id" to null,
                "verify" to verify,
                "type" to EmailTypes.VERIFY_BETA.id,
                "attempts" to 1,
                "email" to email
        ))

        Mongo.getClient()
                .getDatabase("email")
                .getCollection("verify")
                .insertOne(doc)

        val request = UserEmailRequest(null, email, verify, EmailTypes.VERIFY_BETA.id, 1)

        verifyRequests.add(request)

        sendEmail(request, EmailTypes.VERIFY_BETA.default)
    }

    /**
     * Reset a password.
     */
    fun passwordReset(id: Long, verify: String, newPassword: String) {
        verifyRequests.singleOrNull { request ->
            request.type == EmailTypes.VERIFY_PASSWORD_RESET.id
                    && request.id == id
                    && request.verify.equals(verify, true)
        }
                ?: throw InvalidArguments("verify")

        verifyRequests.removeIf { request -> request.verify.equals(verify, true) }

        Mongo.getClient()
                .getDatabase("email")
                .getCollection("verify")
                .deleteOne(Filters.and(eq("id", id), eq("verify", verify)))

        UserManager.getUser(id).password = BCrypt.hashpw(newPassword, BCrypt.gensalt())
    }

    /**
     * Send a password reset email for [id] to [email]
     */
    @Throws(AlreadyVerified::class, Unverified::class)
    fun sendPasswordReset(id: Long) {
        val user = UserManager.getUser(id)

        if (!user.verified)
            throw Unverified()

        val verify = IdGenerator.generateRandomString(32)

        val exists = verifyRequests.singleOrNull { request ->
            request.type == EmailTypes.VERIFY_PASSWORD_RESET.id
                    && request.id == id
        }

        // If there's already an ongoing email request, you can't change it until you've confirmed your first.
        if (exists != null)
            throw NoPermission()

        val doc = Document(mapOf(
                "id" to id,
                "verify" to verify,
                "type" to EmailTypes.VERIFY_PASSWORD_RESET.id,
                "attempts" to 1,
                "email" to user.email
        ))

        Mongo.getClient()
                .getDatabase("email")
                .getCollection("verify")
                .insertOne(doc)

        val request = UserEmailRequest(id, user.email, verify, EmailTypes.VERIFY_PASSWORD_RESET.id, 1)

        verifyRequests.add(request)

        sendEmail(request, EmailTypes.VERIFY_PASSWORD_RESET.default)
    }

    /**
     * Send a verification email for [id] to [email]
     */
    @Throws(AlreadyVerified::class, Unverified::class)
    fun sendVerify(id: Long, email: String) {
        val user = UserManager.getUser(id)

        if (user.verified)
            throw AlreadyVerified()

        val verify = IdGenerator.generateRandomString(32)

        val exists = verifyRequests.singleOrNull { request ->
            request.type == EmailTypes.VERIFY_EMAIL.id
                    && request.id == id
                    && request.email.equals(email, true)
        }

        // If there's already an ongoing email request, you can't change it until you've confirmed your first.
        if (exists != null)
            throw Unverified()

        val doc = Document(mapOf(
                "id" to id,
                "verify" to verify,
                "type" to EmailTypes.VERIFY_EMAIL.id,
                "attempts" to 1,
                "email" to email
        ))

        Mongo.getClient()
                .getDatabase("email")
                .getCollection("verify")
                .insertOne(doc)

        val request = UserEmailRequest(id, email, verify, EmailTypes.VERIFY_EMAIL.id, 1)

        verifyRequests.add(request)

        sendEmail(request, EmailTypes.VERIFY_EMAIL.default)
    }

    /**
     * Unsubscribe an email.
     */
    @Throws(InvalidArguments::class)
    fun unSubscribe(email: String) {
        val contains = unsubscribed.singleOrNull { unsub -> unsub.first.equals(email, true) }

        if (contains != null)
            throw InvalidArguments("email")

        val time = currentTimeMillis()

        val doc = Document(mapOf(
                "email" to email,
                "time" to time
        ))

        Mongo.getClient()
                .getDatabase("email")
                .getCollection("unsubscribed")
                .insertOne(doc)

        unsubscribed.add(email to time)
    }

    /**
     * Handle a bounce request
     *
     * TODO
     */
    fun handleBounce(bounce: String) {
        val obj = JSONObject(bounce)
        val destinations = obj.getJSONArray("destination")

        for (i in 0 until destinations.length())
            unSubscribe(destinations.getString(i))

        unifey.webhook.sendBigMessage(bounce, "A bounce has occurred when sending an email to $destinations")
    }

    /**
     * The max amount of times a user can request a resend.
     */
    private const val MAX_EMAIL_RESEND = 10

    /**
     * Resend an email
     */
    @Throws(TooManyAttempts::class, InvalidArguments::class)
    fun resendEmail(id: Long, type: Int) {
        val request = verifyRequests
                .singleOrNull { request -> request.type == type && request.id == id }
                ?: throw InvalidArguments("type", "id")

        resendEmail(request)
    }

    /**
     * Resend an email.
     */
    @Throws(TooManyAttempts::class)
    fun resendEmail(request: UserEmailRequest) {
        if (request.attempts >= MAX_EMAIL_RESEND)
            throw TooManyAttempts()

        request.attempts += 1

        val type = EmailTypes
                .values()
                .single { type -> type.id == request.type }


        sendEmail(request, type.default)
    }

    /**
     * Send an email.
     */
    private fun sendEmail(request: UserEmailRequest, email: Email) {
        val client = AmazonSimpleEmailServiceClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build()

        val body = Body()
                .withHtml(Content(email.getBody(request)))

        val subject = Content(email.getSubject(request))

        val message = Message()
                .withBody(body)
                .withSubject(subject)

        val emailRequest = SendEmailRequest()
                .withDestination(Destination().withToAddresses(request.email))
                .withMessage(message)
                .withSource("noreply@unifey.net")
                .withConfigurationSetName("unifey")

        try {
            client.sendEmail(emailRequest)
        } catch (ex: Exception) {
            // TODO
            unifey.sendMessage("There was an issue sending an email to ${request.id} (${request.email})")
        }
    }
}