package net.unifey.handle.feeds.posts

import net.unifey.DatabaseHandler

class Post(
        val id: Long,
        val createdAt: Long,
        val authorId: Long,
        val feed: String,
        title: String,
        content: String,
        hidden: Boolean,
        upvotes: Long,
        downvotes: Long
) {
    /**
     * Change a post's title.
     */
    var title = title
        set(value) {
            DatabaseHandler.getConnection()
                    .prepareStatement("UPDATE posts SET title = ? WHERE id = ?")
                    .apply {
                        setString(1, value)
                        setLong(2, id)
                    }
                    .executeUpdate()

            field = value
        }

    /**
     * Change a post's content
     */
    var content = content
        set(value) {
            DatabaseHandler.getConnection()
                    .prepareStatement("UPDATE posts SET content = ? WHERE id = ?")
                    .apply {
                        setString(1, value)
                        setLong(2, id)
                    }
                    .executeUpdate()

            field = value
        }

    /**
     * If the post is hidden.
     */
    var hidden = hidden
        set(value) {
            DatabaseHandler.getConnection()
                    .prepareStatement("UPDATE posts SET hidden = ? WHERE id = ?")
                    .apply {
                        setInt(1, if (value) 1 else 0)
                        setLong(2, id)
                    }
                    .executeUpdate()

            field = value
        }

    /**
     * A post's upvotes
     */
    var upvotes = upvotes
        set(value) {
            DatabaseHandler.getConnection()
                    .prepareStatement("UPDATE posts SET upvotes = ? WHERE id = ?")
                    .apply {
                        setLong(1, value)
                        setLong(2, id)
                    }
                    .executeUpdate()

            field = value
        }

    /**
     * A post's downvotes
     */
    var downvotes = downvotes
        set(value) {
            DatabaseHandler.getConnection()
                    .prepareStatement("UPDATE posts SET downvotes = ? WHERE id = ?")
                    .apply {
                        setLong(1, value)
                        setLong(2, id)
                    }
                    .executeUpdate()

            field = value
        }
}