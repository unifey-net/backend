package net.unifey.handle.feeds.custom

/**
 * A limited feed. This only contains information about page count and post count. This is used for
 * feeds that aren't owned by anything, and are only a collection of other feeds.
 *
 * @see PersonalizedFeed
 */
data class LimitedFeed(val pageCount: Long, val postCount: Long)
