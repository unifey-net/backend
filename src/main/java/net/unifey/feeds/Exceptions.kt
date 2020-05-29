package net.unifey.feeds

import java.lang.Exception

open class FeedException(msg: String): Exception(msg)

class CannotViewFeed: FeedException("You cannot view this feed!")

class CannotPostFeed: FeedException("You cannot post to this feed!")