package net.unifey.handle


// TODO don't use this
open class ApiException(message: String): Exception(message)

class NotFound(obj: String): ApiException("That $obj could not be found!")

class InvalidArguments(vararg args: String): ApiException("Required arguments: ${args.joinToString(", ")}")

class ArgumentTooLarge(arg: String, max: Int): ApiException("$arg must be under $max")

class AlreadyExists(type: String, arg: String): ApiException("A $type with that $arg already exists!")

class NoPermission: ApiException("You don't have permission for this!")