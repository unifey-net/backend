package net.unifey.feeds

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.*
import net.unifey.auth.ex.AuthenticationException
import net.unifey.auth.isAuthenticated
import net.unifey.feeds.posts.PostManager
import net.unifey.response.Response
import kotlin.random.Random

fun Routing.feedPages() {
    route("/feeds/{feed}") {
        get {
            val user = call.isAuthenticated()

            val feed = call.parameters["feed"]

            if (feed == null)
                call.respond(Response("No feed parameter"))
            else {
                val feedObj = FeedManager.getFeed(feed)

                if (feedObj == null)
                    call.respond(HttpStatusCode.BadRequest, Response("Invalid feed object"))
                else
                    call.respond(Response(
                            FeedManager.getFeedPosts(feedObj, user.owner)
                    ))
            }
        }

        post {
            val user = call.isAuthenticated()

            val params = call.receiveParameters()
            val feed = call.parameters["feed"]
            val content = params["content"]
            val title = params["title"]

            val seq = sequenceOf(feed, content, title)

            when {
                seq.any { it == null } ->
                    call.respond(HttpStatusCode.BadRequest, Response("No feed, title or content parameter"))

                seq.any { it!!.isBlank() } ->
                    call.respond(HttpStatusCode.BadRequest, Response("Feed, title or content may be empty"))

                else -> {
                    val feedObj = FeedManager.getFeed(feed!!)

                    if (feedObj == null)
                        call.respond(HttpStatusCode.BadRequest, Response("Invalid feed object"))
                    else
                        call.respond(Response(
                                PostManager.createPost(feedObj, title!!, content!!, user.owner)
                        ))
                }
            }
        }

        delete {
            val user = call.isAuthenticated()

            val params = call.receiveParameters()
            val post = params["post"]?.toLongOrNull()

            if (post == null)
                call.respond(HttpStatusCode.BadRequest, Response("No post parameter"))
            else {
                val postObj = PostManager.getPost(post)

                when {
                    postObj == null ->
                        call.respond(HttpStatusCode.BadRequest, Response("Invalid post ID."))

                    postObj.authorUid != user.owner ->
                        throw AuthenticationException("You are not the owner of this post!")

                    else -> {
                        PostManager.deletePost(postObj.id)

                        call.respond(Response("Post has been deleted."))
                    }
                }
            }
        }
    }
}