package net.unifey.handle.users.email

import com.amazonaws.services.simpleemail.model.*
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Content
import dev.ajkneisl.lib.util.currentTimeMillis
import io.ktor.http.*
import io.ktor.response.*
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import net.unifey.handle.Error
import net.unifey.handle.InvalidArguments
import net.unifey.handle.NotFound
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.UserManager
import net.unifey.handle.users.email.defaults.Email
import net.unifey.response.Response
import net.unifey.util.IdGenerator
import net.unifey.webhook
import org.bson.Document
import org.json.JSONObject
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object UserEmailManager {
    private val SEND_GRID = SendGrid(System.getenv("SENDGRID_API_KEY"))

    /** The verification requests */
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
                    doc.getInteger("type"))
            }
            .toMutableList()
    }

    /** Get an email request. */
    @Throws(NotFound::class)
    fun getRequest(id: Long, type: Int): UserEmailRequest {
        return verifyRequests.firstOrNull { request -> request.id == id && request.type == type }
            ?: throw NotFound("emailRequest")
    }

    /** The users who are unsubscribed */
    private val unsubscribed: MutableList<Pair<String, Long>> by lazy {
        Mongo.getClient()
            .getDatabase("email")
            .getCollection("unsubscribed")
            .find()
            .map { doc -> doc.getString("email") to doc.getLong("time") }
            .toMutableList()
    }

    /** Verify [id] using [verify] */
    @Throws(InvalidArguments::class)
    suspend fun verify(id: Long, verify: String) {
        verifyRequests.singleOrNull { request ->
            request.type == EmailTypes.VERIFY_EMAIL.id &&
                request.id == id &&
                request.verify.equals(verify, true)
        }
            ?: throw InvalidArguments("verify")

        verifyRequests.removeIf { request -> request.verify.equals(verify, true) }

        Mongo.getClient()
            .getDatabase("email")
            .getCollection("verify")
            .deleteOne(Filters.and(eq("id", id), eq("verify", verify)))

        UserManager.getUser(id).verified = true
    }

    /** Reset a password. */
    suspend fun passwordReset(verify: String, newPassword: String) {
        val request =
            verifyRequests.singleOrNull { request ->
                request.type == EmailTypes.VERIFY_PASSWORD_RESET.id &&
                    request.verify.equals(verify, true)
            }
                ?: throw InvalidArguments("verify")

        verifyRequests.removeIf { req -> req.verify.equals(verify, true) }

        Mongo.getClient()
            .getDatabase("email")
            .getCollection("verify")
            .deleteOne(Filters.and(eq("id", request.id!!), eq("verify", verify)))

        UserManager.getUser(request.id).password = BCrypt.hashpw(newPassword, BCrypt.gensalt())
    }

    /** Send a password reset email for [id] to [email] */
    @Throws(AlreadyVerified::class, Unverified::class, Error::class)
    suspend fun sendPasswordReset(id: Long) {
        val user = UserManager.getUser(id)

        if (!user.verified) throw Unverified()

        val verify = IdGenerator.generateRandomString(32)

        val exists =
            verifyRequests.singleOrNull { request ->
                request.type == EmailTypes.VERIFY_PASSWORD_RESET.id && request.id == id
            }

        // If there's already an ongoing email request, you can't change it until you've confirmed
        // your first.
        if (exists != null)
            throw Error({
                respond(
                    HttpStatusCode.Companion.BadRequest,
                    Response(
                        "There's already an outgoing request to reset the password on this account!"))
            })

        val doc =
            Document(
                mapOf(
                    "id" to id,
                    "verify" to verify,
                    "type" to EmailTypes.VERIFY_PASSWORD_RESET.id,
                    "attempts" to 1,
                    "email" to user.email))

        Mongo.getClient().getDatabase("email").getCollection("verify").insertOne(doc)

        val request = UserEmailRequest(id, user.email, verify, EmailTypes.VERIFY_PASSWORD_RESET.id)

        verifyRequests.add(request)

        sendEmail(request, EmailTypes.VERIFY_PASSWORD_RESET.default)
    }

    /** Send a verification email for [id] to [email] */
    @Throws(AlreadyVerified::class, Unverified::class)
    suspend fun sendVerify(id: Long, email: String) {
        val user = UserManager.getUser(id)

        if (user.verified) throw AlreadyVerified()

        val verify = IdGenerator.generateRandomString(32)

        val exists =
            verifyRequests.singleOrNull { request ->
                request.type == EmailTypes.VERIFY_EMAIL.id &&
                    request.id == id &&
                    request.email.equals(email, true)
            }

        // If there's already an ongoing email request, you can't change it until you've confirmed
        // your first.
        if (exists != null) throw Unverified()

        val doc =
            Document(
                mapOf(
                    "id" to id,
                    "verify" to verify,
                    "type" to EmailTypes.VERIFY_EMAIL.id,
                    "attempts" to 1,
                    "email" to email))

        Mongo.getClient().getDatabase("email").getCollection("verify").insertOne(doc)

        val request = UserEmailRequest(id, email, verify, EmailTypes.VERIFY_EMAIL.id)

        verifyRequests.add(request)

        sendEmail(request, EmailTypes.VERIFY_EMAIL.default)
    }

    /** Unsubscribe an email. */
    @Throws(InvalidArguments::class)
    fun unSubscribe(email: String) {
        val contains = unsubscribed.singleOrNull { unsub -> unsub.first.equals(email, true) }

        if (contains != null) throw InvalidArguments("email")

        val time = currentTimeMillis()

        val doc = Document(mapOf("email" to email, "time" to time))

        Mongo.getClient().getDatabase("email").getCollection("unsubscribed").insertOne(doc)

        unsubscribed.add(email to time)
    }

    /** Resend an email */
    @Throws(TooManyAttempts::class, InvalidArguments::class)
    suspend fun resendEmail(id: Long, type: Int) {
        val request =
            verifyRequests.singleOrNull { request -> request.type == type && request.id == id }

        val user = UserManager.getUser(id)

        when {
            request != null -> resendEmail(request)
            !user.verified -> {
                sendVerify(id, user.email)
            }
            else -> throw InvalidArguments("type", "id")
        }
    }

    /** Resend an email. */
    @Throws(TooManyAttempts::class)
    suspend fun resendEmail(request: UserEmailRequest) {
        val type = EmailTypes.values().single { type -> type.id == request.type }

        sendEmail(request, type.default)
    }

    val EMAIL_LOGGER: Logger = LoggerFactory.getLogger(this::class.java)

    /** Send an email. */
    private suspend fun sendEmail(request: UserEmailRequest, email: Email) {
        val from = com.sendgrid.helpers.mail.objects.Email("unifey@ajkneisl.dev")
        val subject = email.getSubject(request)
        val to = com.sendgrid.helpers.mail.objects.Email(request.email)
        val content = Content("text/html", email.getBody(request))
        val mail = Mail(from, subject, to, content)

        val sgRequest = Request()
        sgRequest.method = Method.POST
        sgRequest.endpoint = "mail/send"
        sgRequest.body = mail.build()

        try {
            val response = SEND_GRID.api(sgRequest)

            EMAIL_LOGGER.info(
                "An email (${request.id} - ${request.type}) has been sent to ${request.email}. (${response.statusCode})")
        } catch (ex: IOException) {
            EMAIL_LOGGER.error(
                "An email (${request.id} - ${request.type}) could not be sent to ${request.email}.")
            webhook.sendMessage("There was an issue sending an email to ${request.id} (${request.email})")
        }
    }
}
