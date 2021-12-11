package net.unifey.handle.messaging.channels.objects

/**
 * A group message channel.
 *
 * @param name The name of the group chat. This is changeable by the [owner].
 * @param members The members of the group chat.
 * @param owner The owner of the group chat. This user can kick and invite people.
 * @param description The description of the group chat. This is changeable by [owner].
 *
 */
data class GroupMessageChannel(
    override val id: Long,
    val name: String,
    val description: String,
    val members: ArrayList<Long>,
    val owner: Long,
) : MessageChannel(id, ChannelType.GROUP)