package net.unifey.handle.users

import com.sun.mail.smtp.SMTPTransport
import dev.shog.lib.app.cfg.ConfigHandler
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.*
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.routing.*
import net.unifey.DatabaseHandler
import net.unifey.auth.isAuthenticated
import net.unifey.config.Config
import net.unifey.handle.users.profile.Profile
import net.unifey.response.Response
import net.unifey.util.IdGenerator
import java.util.*
import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

fun Routing.userPages() {
    route("/user") {
        /**
         * Get your own user data.
         */
        get {
            val token = call.isAuthenticated()

            call.respond(Response(UserManager.getUser(token.owner)))
        }

        /**
         * Change our own email
         */
        put("/email") {
            val token = call.isAuthenticated()

            val params = call.receiveParameters()
            val email = params["email"]

            if (email == null)
                call.respond(HttpStatusCode.BadRequest, Response("No email parameter"))
            else {
                UserManager.getUser(token.owner).updateEmail(email)

                when {
                    email.length > 120 ->
                        call.respond(HttpStatusCode.BadRequest, Response("Email is too long! (must be <=120)"))

                    !UserManager.EMAIL_REGEX.matches(email) ->
                        call.respond(HttpStatusCode.BadRequest, Response("Not a proper email!"))

                    else -> {

                        call.respond(HttpStatusCode.OK, Response("Changed email."))
                    }
                }
            }
        }

        put("/verify") {
            val token = call.isAuthenticated();
            val email = UserManager.getUser(token.owner).email;
            val vkey = IdGenerator.generateRandomString(32);

            val rs = DatabaseHandler.getConnection().prepareStatement("INSERT INTO verification (email, vkey) VALUES (?, ?)");
            rs.setString(1, email);
            rs.setString(2, vkey);
            rs.executeQuery();

            val cfg = ConfigHandler.getConfig(ConfigHandler.ConfigType.YML, "unifey").asObject<Config>();
            val prop: Properties = System.getProperties();
            prop.put("mail.smtp.host", cfg.smtpHost);
            prop.put("mail.smtp.auth", "true");
            prop.put("mail.smtp.port", "25");
            val session: Session = Session.getInstance(prop, null);
            val msg: Message = MimeMessage(session);
            msg.setFrom(InternetAddress("noreply@unifey.net"));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email, false));
            msg.subject = "Account verification";
            msg.setText("Please verify your account using the link below:\n\nhttps://unifey.net/verify?email=${email}&vkey=${vkey}");
            msg.sentDate = Date();
            val t = session.getTransport("smtp") as SMTPTransport;
            t.connect(cfg.smtpHost, cfg.smtpUsername, cfg.smtpPassword);
            t.sendMessage(msg, msg.allRecipients);
            t.close();
        }

        get("/verify") {
            val params = call.receiveParameters();
            val email = params.get("email");
            val vkey = params.get("vkey");

            if (email == null || vkey == null)
                call.respond(HttpStatusCode.BadRequest, Response("Missing parameters"));
            else {
                val rs = DatabaseHandler.getConnection().prepareStatement("SELECT * FROM verification WHERE email = ?");
                rs.setString(1, email);
                val res = rs.executeQuery();
                val dbvkey = res.getString("vkey");
                if (!vkey.equals(vkey)) {
                    call.respond(HttpStatusCode.BadRequest, Response("Data mismatch"))
                } else {
                    val rs2 = DatabaseHandler.getConnection().prepareStatement("DELETE FROM verification WHERE vkey = ?");
                    rs2.setString(1, vkey);
                    rs2.executeQuery();

                    call.respond(HttpStatusCode.OK, Response("Email verified"));
                }
            }
        }

        /**
         * Change your own name.
         *
         * TODO check username already exists
         */
        put("/name") {
            val token = call.isAuthenticated()

            val params = call.receiveParameters()
            val username = params["username"]

            if (username == null)
                call.respond(HttpStatusCode.BadRequest, Response("No username parameter"))
            else {
                when {
                    username.length > 16 ->
                        call.respond(HttpStatusCode.BadRequest, Response("Username is too long! (must be <=16)"))

                    3 > username.length ->
                        call.respond(HttpStatusCode.BadRequest, Response("Username is too short! (must be >3)"))

                    else -> {
                        UserManager.getUser(token.owner).username = username

                        call.respond(HttpStatusCode.OK, Response("Changed username."))
                    }
                }
            }
        }

        /**
         * Change your own picture;.
         */
        put("/picture") {
            val token = call.isAuthenticated()

            if (call.request.header("Content-Type") == ContentType.Image.JPEG.toString()) {
                val bytes = call.receiveStream().readBytes()

                if (bytes.size > 4000000) {
                    call.respond(HttpStatusCode.PayloadTooLarge, Response("That picture is too big!"))
                } else {
                    ProfilePictureManager.uploadPicture(token.owner, bytes)

                    call.respond(Response("Uploaded picture successfully"))
                }
                return@put
            }

            call.respond(HttpStatusCode.PayloadTooLarge, Response("Image type is not JPEG!"))
        }

        /**
         * Manage other users using usernames.
         */
        route("/name/{name}") {
            /**
             * Get a user's data.
             */
            get {
                val name = call.parameters["name"]

                if (name == null)
                    call.respond(HttpStatusCode.BadRequest, Response("No name parameter"))
                else
                    call.respond(Response(UserManager.getId(name)))
            }

            /**
             * Get a user's picture.
             */
            get("/picture") {
                val name = call.parameters["name"]

                if (name == null)
                    call.respond(HttpStatusCode.BadRequest, Response("No name parameter"))
                else {
                    call.respondBytes(ProfilePictureManager.getPicture(UserManager.getId(name)), ContentType.Image.JPEG)
                }
            }
        }

        /**
         * Manage other users using IDs.
         */
        route("/id/{id}") {
            get {
                val id = call.parameters["id"]?.toLongOrNull()

                if (id == null)
                    call.respond(HttpStatusCode.BadRequest, Response("No id parameter"))
                else
                    call.respond(Response(UserManager.getUser(id)))
            }

            get("/picture") {
                val id = call.parameters["id"]?.toLongOrNull()

                if (id == null)
                    call.respond(HttpStatusCode.BadRequest, Response("No id parameter"))
                else {
                    call.respondBytes(ProfilePictureManager.getPicture(UserManager.getUser(id).id), ContentType.Image.JPEG)
                }
            }
        }
    }
}