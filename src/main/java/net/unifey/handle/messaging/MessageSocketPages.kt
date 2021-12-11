package net.unifey.handle.messaging

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.unifey.handle.live.SocketActionHandler
import net.unifey.handle.live.SocketActionHandler.action
import net.unifey.handle.live.WebSocket.customTypeMessage
import net.unifey.handle.messaging.channels.ChannelHandler
import net.unifey.handle.messaging.channels.objects.*
import net.unifey.handle.messaging.channels.objects.responses.ChannelResponse

fun messageSocketActions() = SocketActionHandler.socketActions {
    action("SEND_MESSAGE") {
        checkArguments("message" to String::class, "channel" to Long::class)

        val message = data["message"]!! as String
        val channel = data["channel"]!! as Long

        MessageHandler.sendMessage(token, message, channel)

        true
    }

    action("MODIFY_GROUP_CHAT") {
        checkArguments("type" to String::class, "channel" to Long::class)
        val channel = data["channel"] as Long

        when ((data["type"] as String).lowercase()) {
            "remove_member" -> {
                checkArguments("user" to Long::class, "channel" to Long::class)

                ChannelHandler.removeGroupChatMember(token, channel, data["user"]!! as Long)
            }

            "change_name" -> {
                checkArguments("name" to String::class)

                ChannelHandler.changeName(token, channel, data["name"] as String)
            }

            "change_description" -> {
                checkArguments("description" to String::class)

                ChannelHandler.changeDescription(token, channel, data["description"] as String)
            }

            else -> return@action false
        }

        true
    }

    action("GET_CHANNELS") {
        val mapper = jacksonObjectMapper()

        customTypeMessage("CHANNELS", mapper.writeValueAsString(
            ChannelHandler.getUserChannels(token).map { channel ->
                ChannelResponse(
                    channel,
                    MessageHandler.getPageCount(channel.id),
                    MessageHandler.getMessageCount(channel.id)
                )
            })
        )

        true
    }

    action("OPEN_DIRECT_MESSAGE") {
        checkArguments("receiver" to Long::class)

        ChannelHandler.openDirectMessage(token.owner, data["receiver"]!! as Long)

        true
    }

    action("CREATE_GROUP_CHAT") {
        val users = getListArgument<Long>("users")

        ChannelHandler.createGroupChat(token.owner, ArrayList(users))

        true
    }

    action("GET_MESSAGES") {
        checkArguments("channel" to Long::class, "page" to Int::class)
        val mapper = jacksonObjectMapper()

        val messages = MessageHandler.getMessageHistory(data["channel"] as Long, data["page"] as Int)

        customTypeMessage("MESSAGE_HISTORY", mapper.writeValueAsString(messages))

        true
    }

    action("GET_CHANNEL") {
        checkArguments("channel" to Long::class)
        val mapper = jacksonObjectMapper()

        val channel = data["channel"] as Long
        val channelObj = ChannelHandler.getChannel<MessageChannel>(channel)

        customTypeMessage("GET_CHANNEL", mapper.writeValueAsString(
            ChannelResponse(
                if (channelObj.type == ChannelType.GROUP)
                    ChannelHandler.getChannel<GroupMessageChannel>(channel)
                else
                    ChannelHandler.getChannel<DirectMessageChannel>(channel),
                MessageHandler.getPageCount(channel),
                MessageHandler.getMessageCount(channel)
            )
        ))

        true
    }

    action("START_TYPING") {
        checkArguments("channel" to Long::class)

        true
    }
    action("STOP_TYPING") {
        checkArguments("channel" to Long::class)

        true
    }

    action("DELETE_MESSAGE") { true }
    action("EDIT_MESSAGE") { true }

    action("ADD_REACTION") { true }
    action("DELETE_REACTION") { true }

    /**
     * View the history of messages. Works by getting X amount of messages before Y message.
     */
    action("VIEW_HISTORY") { true }
}