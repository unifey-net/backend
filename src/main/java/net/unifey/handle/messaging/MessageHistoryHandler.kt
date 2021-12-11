package net.unifey.handle.messaging

import com.mongodb.client.model.Filters
import net.unifey.handle.mongo.Mongo

object MessageHistoryHandler {
    /**
     * There are 100 messages per grabbable page.
     */
    const val PAGE_SIZE = 100

    fun getPage(sender: Long, receiver: Long, page: Int) {
        Mongo.getClient()
            .getDatabase("users")
            .getCollection("messages")
            .find(Filters.and(Filters.eq("sentTo", receiver) ))
    }
}