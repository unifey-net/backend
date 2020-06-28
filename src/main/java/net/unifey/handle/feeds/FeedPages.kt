package net.unifey.handle.feeds

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.*
import net.unifey.auth.ex.AuthenticationException
import net.unifey.auth.isAuthenticated
import net.unifey.handle.InvalidArguments
import net.unifey.handle.communities.CommunityManager
import net.unifey.handle.communities.CommunityRoles
import net.unifey.handle.communities.responses.GetCommunityResponse
import net.unifey.handle.feeds.posts.PostManager
import net.unifey.handle.feeds.responses.GetFeedResponse
import net.unifey.handle.feeds.responses.GetPostResponse
import net.unifey.handle.users.UserManager
import net.unifey.response.Response

/**
 * Pages for feeds.
 */
fun Routing.feedPages() {
    route("/feeds/{feed}") {
        /**
         * Get a feed and it's posts.
         */
        get {
            val feed = call.parameters["feed"]
                    ?: throw InvalidArguments("feed")

            val feedObj = FeedManager.getFeed(feed)

            if (feedObj.id.startsWith("cf_")) {
                val community = CommunityManager.getCommunity(feedObj.id.removePrefix("cf_").toLongOrNull() ?: -1)

                if (community.viewRole != CommunityRoles.DEFAULT) {
                    val user = call.isAuthenticated()

                    call.respond(GetFeedResponse(
                            feedObj,
                            FeedManager.getFeedPosts(feedObj, user.owner)
                                    .map { GetPostResponse(it, UserManager.getUser(it.authorId)) }
                    ))

                    return@get
                }
            }

            call.respond(GetFeedResponse(
                    feedObj,
                    FeedManager.getFeedPosts(feedObj, null)
                            .map { GetPostResponse(it, UserManager.getUser(it.authorId)) }
            ))
        }

        /**
         * Post to a feed.
         */
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

        /**
         * Delete a post. Only deletes if authorization token is the owner.
         */
        delete {
            val user = call.isAuthenticated()

            val params = call.receiveParameters()
            val post = params["post"]?.toLongOrNull()

            if (post == null)
                call.respond(HttpStatusCode.BadRequest, Response("No post parameter"))
            else {
                val postObj = PostManager.getPost(post)

                when {
                    postObj.authorId != user.owner ->
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