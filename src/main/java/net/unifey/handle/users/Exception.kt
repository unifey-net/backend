package net.unifey.handle.users

class UserNotFound : Exception("That user could not be found!")

class InvalidInput(message: String) : Exception(message)