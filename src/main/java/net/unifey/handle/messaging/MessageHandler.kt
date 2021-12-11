package net.unifey.handle.messaging

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mongodb.client.model.Sorts
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import net.unifey.auth.tokens.Token
import net.unifey.handle.NotFound
import net.unifey.handle.live.Live
import net.unifey.handle.messaging.channels.objects.messages.responses.IncomingMessageResponse
import net.unifey.handle.messaging.channels.objects.messages.Message
import net.unifey.handle.mongo.Mongo
import net.unifey.handle.messaging.channels.ChannelHandler
import net.unifey.handle.messaging.channels.objects.ChannelType
import net.unifey.handle.messaging.channels.objects.DirectMessageChannel
import net.unifey.handle.messaging.channels.objects.GroupMessageChannel
import net.unifey.handle.messaging.channels.objects.MessageChannel
import net.unifey.handle.messaging.channels.objects.messages.responses.MessageHistoryResponse
import net.unifey.handle.users.User
import net.unifey.handle.users.UserManager
import net.unifey.util.PageRateLimit
import net.unifey.util.checkRateLimit
import org.litote.kmongo.descending
import org.litote.kmongo.eq
import org.litote.kmongo.orderBy
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.Throws

object MessageHandler {
    private val LOGGER = LoggerFactory.getLogger(this::class.java)
    private val RATE_LIMIT = PageRateLimit(Bandwidth.classic(5, Refill.greedy(1, Duration.ofSeconds(1))))
    private const val PER_PAGE_COUNT = 100

    /**
     * Send a system message to [channel]
     */
    suspend fun sendSystemMessage(message: String, channel: Long) {
        val channelObject = when (ChannelHandler.getChannel<MessageChannel>(channel).type) {
            ChannelType.GROUP -> ChannelHandler.getChannel<GroupMessageChannel>(channel)
            ChannelType.DIRECT_MESSAGE -> ChannelHandler.getChannel<DirectMessageChannel>(channel)
        }

        val messageObject = Message(
            Message.SYSTEM_ID,
            channelObject.id,
            message,
            System.currentTimeMillis(),
            listOf()
        )

        val response = IncomingMessageResponse(
            channelObject,
            messageObject,
            Message.SYSTEM_ID to Message.SYSTEM_NAME
        )

        val mapper = jacksonObjectMapper()

        Live.sendUpdates {
            users = ChannelHandler.getChannelReceivers(channelObject)
            type = "INCOMING_MESSAGE" // make silent
            data = mapper.writeValueAsString(response)
        }

        Mongo.K_MONGO
            .getDatabase("messages")
            .getCollection<Message>("messages")
            .insertOne(messageObject)
    }

    /**
     * Send a [message] to [channel]
     *
     * @throws NotFound If the channel isn't found.
     */
    @Throws(NotFound::class)
    suspend fun sendMessage(user: Token, message: String, channel: Long) {
        val owner = user.getOwner()

        checkRateLimit(user, RATE_LIMIT)

        // TODO
        val channelObject = when (ChannelHandler.getChannel<MessageChannel>(channel).type) {
            ChannelType.GROUP -> ChannelHandler.getChannel<GroupMessageChannel>(channel)
            ChannelType.DIRECT_MESSAGE -> ChannelHandler.getChannel<DirectMessageChannel>(channel)
        }

        val messageObject = Message(
            user.owner,
            channelObject.id,
            message,
            System.currentTimeMillis(),
            listOf()
        )

        val response = IncomingMessageResponse(
            channelObject,
            messageObject,
            user.owner to owner.username
        )

        val mapper = jacksonObjectMapper()

        Live.sendUpdates {
            users = ChannelHandler.getChannelReceivers(channelObject).filter { id -> id != user.owner }
            type = "INCOMING_MESSAGE"
            data = mapper.writeValueAsString(response)
        }

        Mongo.K_MONGO
            .getDatabase("messages")
            .getCollection<Message>("messages")
            .insertOne(messageObject)
    }

    /**
     * Get the amount of messages in a [channel].
     */
    suspend fun getMessageCount(channel: Long): Int {
        return Mongo.K_MONGO
            .getDatabase("messages")
            .getCollection<Message>("messages")
            .find(Message::channel eq channel)
            .toList()
            .size
    }

    /**
     * Get the amount of pages in a [channel].
     */
    suspend fun getPageCount(channel: Long): Int {
        return kotlin.math.ceil(getMessageCount(channel).toDouble() / PER_PAGE_COUNT.toDouble()).toInt()
    }

    /**
     * Get a message by it's [page] number;
     */
    suspend fun getMessages(channel: Long, page: Int): List<Message> {
        val messageCount = getMessageCount(channel)
        val pageCount = getPageCount(channel)

        if (page > pageCount)
            throw NotFound("page")

        val skip = if (page != 1)
            (pageCount - 1) * 100
        else 0

        val response = Mongo.K_MONGO
            .getDatabase("messages")
            .getCollection<Message>("messages")
            .find(Message::channel eq channel)
            .sort(descending(Message::time))
            .skip(skip)
            .limit(PER_PAGE_COUNT)
            .toList()
            .reversed()

        LOGGER.debug("MESSAGE HISTORY: ($channel -> $page) SKIP = $skip, RESPONSE SIZE = ${response.size}, CHANNEL PG COUNT = $pageCount, CHANNEL MSG COUNT = $messageCount")

        return response
    }

    suspend fun getMessageHistory(channel: Long, page: Int): MessageHistoryResponse {
        val channelObject = ChannelHandler.getChannel<MessageChannel>(channel)

        return MessageHistoryResponse(
            channel,
            page,
            getPageCount(channel),
            getMessages(channel, page).map { message ->
                IncomingMessageResponse(
                    channelObject,
                    message,
                    message.user to UserManager.getUser(message.user).username // usermanager caches, shouldn't hit performance
                )
            }
        )
    }
}