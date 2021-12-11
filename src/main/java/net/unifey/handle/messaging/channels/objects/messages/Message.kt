package net.unifey.handle.messaging.channels.objects.messages

data class Message(
    val user: Long,
    val channel: Long,
    val message: String,
    val time: Long,
    val reactions: List<Long>
) {
    companion object {
        const val SYSTEM_ID = 0L
        const val SYSTEM_NAME = "Harold"
    }
}