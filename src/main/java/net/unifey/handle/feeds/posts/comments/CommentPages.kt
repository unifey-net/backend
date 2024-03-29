package net.unifey.handle.feeds.posts.comments

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import net.unifey.auth.tokens.Token
import net.unifey.handle.InvalidArguments
import net.unifey.handle.MalformedContent
import net.unifey.handle.NoPermission
import net.unifey.handle.feeds.SortingMethod
import net.unifey.handle.feeds.getFeed
import net.unifey.handle.feeds.posts.Post
import net.unifey.handle.feeds.posts.getPost
import net.unifey.handle.feeds.posts.vote.VoteManager
import net.unifey.handle.users.UserManager
import net.unifey.response.Response
import net.unifey.util.cleanInput

/** Managing comments on a post. */
fun Route.commentPages() {
    @Throws(InvalidArguments::class)
    suspend fun ApplicationCall.createComment(): Triple<Token, Post, String> {
        val (token, post) = getPost(requirePost = true)

        val content = receiveParameters()["content"] ?: throw InvalidArguments("content")

        // token should never be null. it will always have thrown a NoPermission before.
        return Triple(token ?: throw MalformedContent(), post, cleanInput(content))
    }

    /** Get a comment. */
    get {
        val (token, post) = call.getPost()

        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1

        val sort =
            try {
                val sortString = call.request.queryParameters["sort"] ?: "new"

                SortingMethod.valueOf(sortString.uppercase())
            } catch (ex: Exception) {
                throw InvalidArguments("sort")
            }

        call.respond(CommentManager.getPostCommentData(post, page, sort, token?.owner))
    }

    /** Create a comment. */
    put {
        val (token, post, content) = call.createComment()

        CommentManager.createComment(
            post.id,
            null,
            post.feed,
            UserManager.getUser(token.owner),
            content
        )

        call.respond(Response("OK"))
    }

    /** Manage comments. */
    route("/{comment}") {
        /** Manage a comment. */
        suspend fun ApplicationCall.manageComment(
            requireManage: Boolean = true
        ): Triple<Token, Comment, Post> {
            val (token, post) = getPost()

            if (token == null) throw NoPermission()

            val comment = parameters["comment"]?.toLongOrNull() ?: throw InvalidArguments("comment")

            val obj = CommentManager.getCommentById(comment)

            if (!CommentManager.canManageComment(token.owner, obj) && requireManage)
                throw NoPermission()

            return Triple(token, obj, post)
        }

        /** Get all comments on the included comments. */
        get {
            val (token) = call.getPost()

            val comment =
                call.parameters["comment"]?.toLongOrNull() ?: throw InvalidArguments("comment")

            val page = call.request.queryParameters["id"]?.toIntOrNull() ?: 1

            val obj = CommentManager.getCommentById(comment)

            if (obj.level == 2) throw InvalidArguments("comment")

            call.respond(CommentManager.getCommentData(obj, page, SortingMethod.OLD, token?.owner))
        }

        /** Delete a comment. */
        delete {
            val (_, obj) = call.manageComment()

            CommentManager.deleteComment(obj)

            call.respond(Response("OK"))
        }

        /** Manage content */
        post("/content") {
            val (token, obj) = call.manageComment()

            // must be owner to change content.
            if (token.owner != obj.authorId) throw NoPermission()

            var content = call.receiveParameters()["content"] ?: throw InvalidArguments("content")

            content = cleanInput(content)

            if (content.length > CommentLimits.MAX_COMMENT_LEN) throw InvalidArguments("content")

            CommentManager.setContent(obj.id, content)

            call.respond(Response("OK"))
        }

        /** Manage your own vote. */
        post("/vote") {
            val (token, _) = call.getPost()

            if (token == null) throw NoPermission()

            val comment =
                call.parameters["comment"]?.toLongOrNull() ?: throw InvalidArguments("comment")

            val obj = CommentManager.getCommentById(comment)

            val params = call.receiveParameters()
            val vote = params["vote"]?.toIntOrNull() ?: throw InvalidArguments("vote")

            VoteManager.setCommentVote(obj.id, token.owner, vote)

            call.respond(Response("OK"))
        }

        /** Comment on a comment. */
        put {
            call.getFeed(requireComment = true)

            val (token, obj, post) = call.manageComment(false)

            if (obj.level == 2) throw InvalidArguments("comments")

            var content = call.receiveParameters()["content"] ?: throw InvalidArguments("content")

            content = cleanInput(content)

            CommentManager.createComment(
                post.id,
                obj.id,
                obj.feed,
                UserManager.getUser(token.owner),
                content
            )

            call.respond(Response("OK"))
        }
    }
}
