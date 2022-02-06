package net.unifey.handle.messaging

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import net.unifey.handle.live.SocketAction
import net.unifey.handle.live.SocketActionHandler
import net.unifey.handle.live.SocketActionHandler.action
import net.unifey.handle.live.WebSocket.customTypeMessage
import net.unifey.handle.live.objs.ActionHolder
import net.unifey.handle.live.objs.FindActions
import net.unifey.handle.live.objs.SocketType
import net.unifey.handle.messaging.channels.ChannelHandler
import net.unifey.handle.messaging.channels.objects.ChannelType
import net.unifey.handle.messaging.channels.objects.DirectMessageChannel
import net.unifey.handle.messaging.channels.objects.GroupMessageChannel
import net.unifey.handle.messaging.channels.objects.MessageChannel
import net.unifey.handle.messaging.channels.objects.responses.ChannelResponse

val format = Json {
    serializersModule =
        SerializersModule {
            polymorphic(MessageChannel::class) {
                subclass(DirectMessageChannel::class)
                subclass(GroupMessageChannel::class)
            }
        }
}

/** All the socket actions for messages and channels. */
@FindActions
object MessageSocketPages : ActionHolder {
    override val pages: ArrayList<Pair<SocketType, SocketAction>> =
        SocketActionHandler.socketActions {
            // Send a message to a channel.
            action(ChannelActions.SEND_MESSAGE) {
                checkArguments("message" to String::class, "channel" to Long::class)

                val message = data["message"]!! as String
                val channel = data["channel"]!! as Long

                MessageHandler.sendMessage(token, message, channel)
            }

            // Remove a member, change the name, or change the description of a group chat.
            action(ChannelActions.MODIFY_GROUP_CHAT) {
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

                        ChannelHandler.changeDescription(
                            token,
                            channel,
                            data["description"] as String
                        )
                    }
                }
            }

            // Get all the user's channels.
            action(ChannelActions.GET_CHANNELS) {
                val channels =
                    ChannelHandler.getUserChannels(token).map { channel ->
                        ChannelResponse(
                            channel,
                            MessageHandler.getPageCount(channel.id),
                            MessageHandler.getMessageCount(channel.id)
                        )
                    }

                customTypeMessage("CHANNELS", format.encodeToString(channels))
            }

            // Open a direct message channel with someone.
            action(ChannelActions.OPEN_DIRECT_MESSAGE) {
                checkArguments("receiver" to Long::class)

                ChannelHandler.openDirectMessage(token.owner, data["receiver"]!! as Long)
            }

            // Create a group chat with a group of other users.
            action(ChannelActions.CREATE_GROUP_CHAT) {
                val users = getListArgument<Long>("users")

                ChannelHandler.createGroupChat(token.owner, ArrayList(users))
            }

            // Get message history.
            action(ChannelActions.GET_MESSAGES) {
                checkArguments("channel" to Long::class, "page" to Int::class)

                val messages =
                    MessageHandler.getMessageHistory(data["channel"] as Long, data["page"] as Int)

                customTypeMessage(
                    ChannelUpdateTypes.MESSAGE_HISTORY.toString(),
                    Json.encodeToString(messages)
                )
            }

            // Get a channel by it's ID.
            action(ChannelActions.GET_CHANNEL) {
                checkArguments("channel" to Long::class)

                val channel = data["channel"] as Long
                val channelObj = ChannelHandler.getChannel<MessageChannel>(channel)

                val response =
                    ChannelResponse(
                        if (channelObj.channelType == ChannelType.GROUP)
                            ChannelHandler.getChannel<GroupMessageChannel>(channel)
                        else ChannelHandler.getChannel<DirectMessageChannel>(channel),
                        MessageHandler.getPageCount(channel),
                        MessageHandler.getMessageCount(channel)
                    )

                customTypeMessage("GET_CHANNEL", Json.encodeToString(response))
            }

            // Start typing in a channel.
            action(ChannelActions.START_TYPING) {
                checkArguments("channel" to Long::class)

                ChannelHandler.startTyping(data["channel"] as Long, token.owner)
            }

            // Stop typing in a channel.
            action(ChannelActions.STOP_TYPING) {
                checkArguments("channel" to Long::class)

                ChannelHandler.stopTyping(data["channel"] as Long, token.owner)
            }

            // Delete a message by it's ID
            action(ChannelActions.DELETE_MESSAGE) {
                checkArguments("id" to Long::class)

                MessageHandler.deleteMessage(data["id"] as Long, token.owner)
            }
        }
}
