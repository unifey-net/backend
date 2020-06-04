package net.unifey.handle.feeds

import java.lang.Exception

open class FeedException(msg: String): Exception(msg)

class CannotViewFeed: FeedException("You cannot view this feed!")

class CannotPostFeed: FeedException("You cannot post to this feed!")

class PostDoesntExist: Exception("This post doesn't exist!")