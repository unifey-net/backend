package net.unifey.handle.reports

import net.unifey.handle.InvalidArguments

enum class TargetType {
    POST,
    COMMENT,
    ACCOUNT
}

data class Target(val id: Long, val type: TargetType) {
    companion object {
        fun parse(id: String?, type: String?): Target {
            val parsedId = id?.toLongOrNull()
            val parsedType =
                TargetType.values().singleOrNull { targetType ->
                    targetType.toString().equals(type, true)
                }

            if (parsedId == null || parsedType == null)
                throw InvalidArguments("targetId", "targetType")

            return Target(parsedId, parsedType)
        }
    }
}
