package net.unifey.handle.feeds

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.*
import net.unifey.auth.ex.AuthenticationException
import net.unifey.auth.isAuthenticated
import net.unifey.handle.InvalidArguments
import net.unifey.handle.InvalidVariableInput
import net.unifey.handle.NoPermission
import net.unifey.handle.communities.CommunityManager
import net.unifey.handle.communities.CommunityRoles
import net.unifey.handle.emotes.EmoteHandler
import net.unifey.handle.feeds.posts.PostManager
import net.unifey.handle.feeds.posts.vote.VoteManager
import net.unifey.handle.feeds.responses.GetFeedResponse
import net.unifey.handle.feeds.responses.GetPostResponse
import net.unifey.handle.users.UserManager
import net.unifey.response.Response

/**
 * Pages for feeds.
 */
fun Routing.feedPages() {
    route("/feeds") {
        /**
         * Get a personalized list of subscribed communities.
         */
        get("/self") {
            TODO()
        }

        /**
         * Posts that are currently trending.
         * Basically like a top of the day across all communities.
         */
        get("/trending") {
            TODO()
        }

        route("/{feed}") {
            /**
             * Get a feed and it's posts.
             */
            get {
                val feed = call.parameters["feed"]
                        ?: throw InvalidArguments("feed")

                val feedObj = FeedManager.getFeed(feed)

                call.respond(feedObj)
            }

            post("/vote") {
                val token = call.isAuthenticated()

                val params = call.receiveParameters()

                val vote = params["vote"]?.toIntOrNull()
                val post = params["post"]?.toLongOrNull()

                if (vote == null || post == null)
                    throw InvalidArguments("vote", "post")

                VoteManager.setVote(post, token.owner, vote)

                call.respond(Response())
            }

            /**
             * A feed object and it's posts.
             */
            get("/posts") {
                val token = try {
                    call.isAuthenticated()
                } catch (ex: Exception) {
                    null
                }

                val feed = call.parameters["feed"]

                val params = call.request.queryParameters

                val page = params["page"]?.toIntOrNull()
                val sort = params["sort"]

                if (feed == null || page == null || sort == null)
                    throw InvalidArguments("sort", "page", "feed")

                val feedObj = FeedManager.getFeed(feed)

                if (feedObj.id.startsWith("cf_")) {
                    val community = CommunityManager.getCommunityById(feedObj.id.removePrefix("cf_").toLongOrNull() ?: -1)

                    if (community.viewRole != CommunityRoles.DEFAULT) {
                        if (token == null)
                            throw NoPermission()

                        val userRole = community.getRole(token.owner)
                                ?: CommunityRoles.DEFAULT

                        if (community.viewRole > userRole)
                            throw NoPermission()

                        val posts = FeedManager.getFeedPosts(feedObj, token.owner, page, sort)
                                .map { post ->
                                    GetPostResponse(
                                            post,
                                            UserManager.getUser(post.authorId),
                                            VoteManager.getVote(post.id, token.owner)
                                    )
                                }

                        call.respond(GetFeedResponse(feedObj, posts))
                        return@get
                    }
                }

                call.respond(GetFeedResponse(
                        feedObj,
                        FeedManager.getFeedPosts(feedObj, null, page, sort)
                                .map {
                                    val vote = if (token != null)
                                        VoteManager.getVote(it.id, token.owner)
                                    else
                                        null

                                    GetPostResponse(it, UserManager.getUser(it.authorId), vote)
                                }
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

                if (feed == null || content == null || title == null)
                    throw InvalidArguments("feed", "content", "title")

                if (sequenceOf(feed, content, title).any(String::isBlank))
                    throw InvalidArguments("feed", "content", "title")

                val feedObj = FeedManager.getFeed(feed)

                call.respond(PostManager.createPost(feedObj, title, content, user.owner))
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
}