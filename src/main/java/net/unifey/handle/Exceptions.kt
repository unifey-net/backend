package net.unifey.handle

import java.lang.Exception

/**
 * [obj] was not found.
 */
class NotFound(val obj: String = ""): Throwable()

/**
 * Required [args] for making request.
 */
class InvalidArguments(vararg val args: String): Throwable()

/**
 * [arg] is too large. (over [max])
 */
class ArgumentTooLarge(val arg: String, val max: Int): Throwable()

/**
 * [type] with [arg] already exists.
 */
class AlreadyExists(val type: String, val arg: String): Throwable()

/**
 * User doesn't have permission.
 */
class NoPermission: Exception("You don't have permission for this!")