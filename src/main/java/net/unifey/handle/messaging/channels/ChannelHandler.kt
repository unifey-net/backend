package net.unifey.handle.messaging.channels

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mongodb.client.model.Filters
import net.unifey.auth.tokens.Token
import net.unifey.handle.AlreadyExists
import net.unifey.handle.InvalidVariableInput
import net.unifey.handle.NoPermission
import net.unifey.handle.NotFound
import net.unifey.handle.live.Live
import net.unifey.handle.messaging.MessageHandler
import net.unifey.handle.messaging.channels.objects.ChannelType
import net.unifey.handle.messaging.channels.objects.DirectMessageChannel
import net.unifey.handle.messaging.channels.objects.GroupMessageChannel
import net.unifey.handle.messaging.channels.objects.MessageChannel
import net.unifey.handle.messaging.channels.objects.responses.GroupChatKickResponse
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.users.ShortUser
import net.unifey.handle.users.UserManager
import net.unifey.util.IdGenerator
import net.unifey.util.toDocument
import org.bson.Document
import org.json.JSONObject
import org.litote.kmongo.*
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.collections.ArrayList
import kotlin.jvm.Throws

object ChannelHandler {
    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    /**
     * Get [user]'s channels.
     */
    suspend fun getUserChannels(user: Token): List<MessageChannel> {
        return getUserDirectMessageChannels(user.owner)
            .plus(getGroupMessageChannelByMember(user.owner))
    }

    /**
     * Get all of the group chats that [user] is in.
     */
    private suspend fun getGroupMessageChannelByMember(user: Long): List<GroupMessageChannel> {
        return Mongo.K_MONGO
            .getDatabase("messages")
            .getCollection<GroupMessageChannel>("channels")
            .find(and(GroupMessageChannel::type eq ChannelType.GROUP, GroupMessageChannel::members contains user))
            .toList()
    }

    /**
     * Get all of the direct message channels that [user] is in.
     */
    private suspend fun getUserDirectMessageChannels(user: Long): List<DirectMessageChannel> {
        return Mongo.K_MONGO
            .getDatabase("messages")
            .getCollection<DirectMessageChannel>("channels")
            .find(and(DirectMessageChannel::type eq ChannelType.DIRECT_MESSAGE, DirectMessageChannel::users contains user))
            .toList()
    }

    /**
     * Get a message channel by it's [id]. [T] should follow either [GroupMessageChannel] or [DirectMessageChannel].
     */
    @Throws(NotFound::class)
    suspend inline fun <reified T : MessageChannel> getChannel(id: Long): T {
        return Mongo.K_MONGO
            .getDatabase("messages")
            .getCollection<T>("channels")
            .find(MessageChannel::id eq id)
            .first()
            ?: throw NotFound("channel")
    }

    /**
     * Get a [T] from a [id].
     */
    inline fun <reified T : MessageChannel> getChannel(document: Document): T {
        val mapper = jacksonObjectMapper()

        return mapper.readValue(document.toJson(), T::class.java)
    }

    /**
     * Get the users of a group chat who should receive updates.
     * (ex: new messages)
     */
    fun getChannelReceivers(channel: MessageChannel): List<Long> {
        return when (channel.type) {
            ChannelType.GROUP ->
                (channel as GroupMessageChannel).members
            ChannelType.DIRECT_MESSAGE ->
                (channel as DirectMessageChannel).users
        }
    }

    /**
     * Check if [id] exists.
     */
    suspend fun channelExists(id: Long): Boolean {
        return try {
            getChannel<MessageChannel>(id)

            true
        } catch (ex: NotFound) {
            false
        }
    }

    /**
     * Get a direct message chanel's id by it's [userOne] and [userTwo].
     *
     * @throws NotFound If there's not a channel between [userOne] and [userTwo]
     */
    @Throws(NotFound::class)
    fun getDirectMessageChannelId(userOne: Long, userTwo: Long): Long {
        val channel = Mongo.getClient()
            .getDatabase("messages")
            .getCollection("channels")
            .find(Filters.and(Filters.`in`("users", userOne), Filters.`in`("users", userTwo)))
            .firstOrNull()
            ?: throw NotFound("channel")

        return channel.getLong("id")
    }

    /**
     * Generate a unique ID for the channel.
     */
    private suspend fun generateIdentifier(): Long {
        return IdGenerator.getSuspensefulId { id ->
            try {
                getChannel<MessageChannel>(id)
                return@getSuspensefulId true
            } catch (ex: NotFound) {
                return@getSuspensefulId false
            }
        }
    }

    /**
     * Create a group chat with [users].
     *
     * [users] must be:
     *  - 1 or more people.
     *  - all be friends with [creator].
     *
     *  @throws NoPermission If [creator] doesn't have all [users]'s friended.
     */
    @Throws(NoPermission::class)
    suspend fun createGroupChat(creator: Long, users: ArrayList<Long>) {
        val user = UserManager.getUser(creator)

        val friends = user.getFriends()
            .map { friend -> friend.id }

        if (!friends.containsAll(users))
            throw NoPermission()

        users.add(creator)

        val channel = GroupMessageChannel(generateIdentifier(), "${user.username}'s group chat", ":)", users, creator)

        val mapper = jacksonObjectMapper()

        Mongo.getClient()
            .getDatabase("messages")
            .getCollection("channels")
            .insertOne(mapper.writeValueAsString(channel).toDocument())

        LOGGER.info("New Channel - GROUP (${channel.id}): ${user.id} $users")
    }

    /**
     * Open a direct message chat between [userOne] and [userTwo].
     *
     * [userOne] (<- the creator) must be friends with [userTwo].
     *
     * @throws NoPermission If [userOne] isn't friends with [userTwo]
     * @throws AlreadyExists If a direct message channel already exists between the two people.
     */
    @Throws(NoPermission::class)
    suspend fun openDirectMessage(userOne: Long, userTwo: Long): Unit {
        val user = UserManager.getUser(userOne)

        when {
            !user.hasFriend(userTwo) -> {
                throw NoPermission()
            }

            try {
                getDirectMessageChannelId(userOne, userTwo)

                true
            } catch (ex: NotFound) {
                false
            } -> {
                throw AlreadyExists("channel", "ID")
            }
        }

        val channel = DirectMessageChannel(generateIdentifier(), arrayListOf(userOne, userTwo))

        val mapper = jacksonObjectMapper()

        Mongo.getClient()
            .getDatabase("messages")
            .getCollection("channels")
            .insertOne(mapper.writeValueAsString(channel).toDocument())

        LOGGER.info("New Channel - DM (${channel.id}): ${user.id} -> $userTwo")
    }

    /**
     * The range of the name of the group chats name lengths.
     */
    private val GROUP_CHAT_NAME_LENGTH = 32 downTo 1

    /**
     * The range of the description of the group chats description lengths.
     */
    private val GROUP_CHAT_DESCRIPTION_LENGTH = 256 downTo 1

    /**
     * Change the name of the group chat [id] to [name]
     *
     * @throws NotFound If [id] isn't a channel.
     * @throws NoPermission If [token] isn't owner
     * @throws InvalidVariableInput If [name] doesn't fit in [GROUP_CHAT_NAME_LENGTH]
     */
    @Throws(NotFound::class, NoPermission::class, InvalidVariableInput::class)
    suspend fun changeName(token: Token, id: Long, name: String) {
        val channel = getChannel<GroupMessageChannel>(id)

        when {
            channel.owner != token.owner ->
                throw NoPermission()

            !GROUP_CHAT_NAME_LENGTH.contains(name.length) ->
                throw InvalidVariableInput("name", "Must be within $GROUP_CHAT_NAME_LENGTH")
        }

        Mongo.K_MONGO
            .getDatabase("messages")
            .getCollection<GroupMessageChannel>("channels")
            .updateOne(GroupMessageChannel::id eq id, setValue(GroupMessageChannel::name, name))

        MessageHandler.sendSystemMessage("The name of the group chat has been updated!", id)

        Live.sendUpdates {
            users = channel.members
            data = name
            type = "GROUP_CHAT_CHANGE_NAME"
        }
    }

    /**
     * Change the description of the group chat [id] to [description]
     *
     * @throws NotFound If [id] isn't a channel.
     * @throws NoPermission If [token] isn't owner
     * @throws InvalidVariableInput If [description] doesn't fit in [GROUP_CHAT_DESCRIPTION_LENGTH]
     */
    @Throws(NotFound::class, NoPermission::class, InvalidVariableInput::class)
    suspend fun changeDescription(token: Token, id: Long, description: String) {
        val channel = getChannel<GroupMessageChannel>(id)

        when {
            channel.owner != token.owner ->
                throw NoPermission()

            !GROUP_CHAT_DESCRIPTION_LENGTH.contains(description.length) ->
                throw InvalidVariableInput("description", "Must be within $GROUP_CHAT_DESCRIPTION_LENGTH")
        }

        Mongo.K_MONGO
            .getDatabase("messages")
            .getCollection<GroupMessageChannel>("channels")
            .updateOne(GroupMessageChannel::id eq id, setValue(GroupMessageChannel::description, description))

        MessageHandler.sendSystemMessage("The description of the group chat has been updated!", id)

        Live.sendUpdates {
            users = channel.members
            data = description
            type = "GROUP_CHAT_CHANGE_DESCRIPTION"
        }
    }

    /**
     * Remove [user] from [id]
     *
     * @throws NotFound If [user] isn't a member.
     * @throws NoPermission [token] isn't owner or [user] == [token::owner]
     */
    @Throws(NotFound::class, NoPermission::class)
    suspend fun removeGroupChatMember(token: Token, id: Long, user: Long) {
        val channel = getChannel<GroupMessageChannel>(id)

        when {
            channel.owner != token.owner ->
                throw NoPermission()

            token.owner == user ->
                throw NoPermission()

            !channel.members.contains(user) ->
                throw NotFound("user")
        }

        channel.members.remove(user) // so it doesn't show up in response

        Mongo.K_MONGO
            .getDatabase("messages")
            .getCollection<GroupMessageChannel>("channels")
            .updateOne(GroupMessageChannel::id eq id, pull(GroupMessageChannel::members, user))

        val userObj = UserManager.getUser(user)

        MessageHandler.sendSystemMessage("${userObj.username} has been kicked from the group!", id)

        Live.sendUpdates {
            users = channel.members.filter { member -> member != user }
            data = GroupChatKickResponse(channel, ShortUser.fromUser(userObj))
            type = "REMOVE_GROUP_CHAT_USER"
        }
    }

    /**
     * Delete a channel by it's [id].
     *
     * This involves deleting all of the messages sent as well.
     */
    fun deleteChannel(id: Long): Unit = TODO()

    /**
     * Change ownership of [id] to [newOwner]. [id] MUST be a group chat.
     */
    fun changeOwnership(id: Long, newOwner: Long): Unit = TODO()
}