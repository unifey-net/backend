package net.unifey.handle.feeds

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.*
import net.unifey.auth.ex.AuthenticationException
import net.unifey.auth.isAuthenticated
import net.unifey.auth.tokens.Token
import net.unifey.handle.InvalidArguments
import net.unifey.handle.NoPermission
import net.unifey.handle.communities.CommunityManager
import net.unifey.handle.communities.CommunityRoles
import net.unifey.handle.feeds.posts.PostLimits
import net.unifey.handle.feeds.posts.PostManager
import net.unifey.handle.feeds.posts.comments.commentPages
import net.unifey.handle.feeds.posts.getPost
import net.unifey.handle.feeds.posts.vote.VoteManager
import net.unifey.handle.feeds.responses.GetFeedResponse
import net.unifey.handle.feeds.responses.GetPostResponse
import net.unifey.handle.users.UserManager
import net.unifey.response.Response
import net.unifey.util.cleanInput

fun ApplicationCall.getFeed(
        requireView: Boolean = false,
        requirePost: Boolean = false,
        requireComment: Boolean = false
): Pair<Feed, Token?> {
    val token = try {
        isAuthenticated()
    } catch (ex: Throwable) {
        null
    }

    val feedStr = parameters["feed"]
            ?: throw InvalidArguments("feed")

    val feed = FeedManager.getFeed(feedStr)

    if (requireView || requirePost) {
        val cantView = requireView && !FeedManager.canViewFeed(feed, token?.owner)
        val cantPost = requirePost && !FeedManager.canPostFeed(feed, token?.owner)
        val cantComment = requireComment && !FeedManager.canCommentFeed(feed, token?.owner)

        if (cantView || cantPost || cantComment)
            throw NoPermission()
    }

    return feed to token
}

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
                val (feed) = call.getFeed()

                call.respond(feed)
            }

            /**
             * Manage and view posts.
             */
            route("/post/{post}") {
                /**
                 * Manage comments
                 */
                route("/comments") {
                    commentPages()
                }

                /**
                 * Get the post
                 */
                get {
                    call.getFeed(requireView = true)

                    val (token, post) = call.getPost()

                    val vote = if (token != null)
                        VoteManager.getVote(post.id, token.owner)
                    else null

                    call.respond(
                            GetPostResponse(post, UserManager.getUser(post.authorId), vote)
                    )
                }

                /**
                 * Manage your own vote.
                 */
                post("/vote") {
                    val (token, post) = call.getPost()

                    if (token == null)
                        throw NoPermission()

                    val params = call.receiveParameters()
                    val vote = params["vote"]?.toIntOrNull()
                            ?: throw InvalidArguments("vote")

                    VoteManager.setVote(post.id, token.owner, vote)

                    call.respond(Response())
                }
            }

            /**
             * A feed object and it's posts.
             */
            get("/posts") {
                val (feed, token) = call.getFeed(requireView = true)

                val params = call.request.queryParameters

                val page = params["page"]?.toIntOrNull()
                val sort = params["sort"]

                if (page == null || sort == null)
                    throw InvalidArguments("sort", "page")

                val response = FeedManager.getFeedPosts(feed, page, sort)
                        .map {
                            val vote = if (token != null)
                                VoteManager.getVote(it.id, token.owner)
                            else
                                null

                            GetPostResponse(it, UserManager.getUser(it.authorId), vote)
                        }

                call.respond(GetFeedResponse(feed, response))
            }

            /**
             * Post to a feed.
             */
            post {
                val (feed, token) = call.getFeed(requirePost = true)

                if (token == null)
                    throw NoPermission()

                val params = call.receiveParameters()

                val preContent = params["content"]
                val preTitle = params["title"]

                if (preContent == null || preTitle == null)
                    throw InvalidArguments("content", "title")

                val content = cleanInput(preContent)
                val title = cleanInput(preTitle)

                when {
                    sequenceOf(content, title).any(String::isBlank) ->
                        throw InvalidArguments("feed", "content", "title")

                    title.length > PostLimits.MAX_TITLE_LEN || content.length > PostLimits.MAX_CONTENT_LEN ->
                        throw InvalidArguments("title", "content")
                }

                call.respond(PostManager.createPost(feed, title, content, token.owner))
            }

            /**
             * Delete a post. Only deletes if authorization token is the owner.
             */
            delete {
                val (token, post, feed) = call.getPost()

                when {
                    token == null ->
                        throw NoPermission()

                    !feed.moderators.contains(token.owner) && token.owner != post.authorId ->
                        throw NoPermission()
                }

                PostManager.deletePost(post.id)

                call.respond(Response())
            }
        }
    }
}