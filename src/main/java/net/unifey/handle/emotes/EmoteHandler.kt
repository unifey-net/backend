package net.unifey.handle.emotes

import kotlinx.coroutines.runBlocking
import net.unifey.handle.AlreadyExists
import net.unifey.handle.InvalidArguments
import net.unifey.handle.LimitReached
import net.unifey.handle.S3ImageHandler
import net.unifey.handle.communities.Community
import net.unifey.handle.communities.CommunityManager
import net.unifey.handle.mongo.MONGO
import net.unifey.util.IdGenerator
import net.unifey.util.URL
import net.unifey.util.cleanInput

/** Handles emotes. */
object EmoteHandler {
    private val EMOTE_NAME_REGEX = Regex("^[A-Za-z0-9-_]{2,16}\\w+$")
    private val EMOTE_URL = "${URL}/emote/viewer/%s/%s"

    private const val MAX_EMOTE_PER_COMMUNITY = 50

    private val emoteCache: MutableList<Emote> by lazy {
        runBlocking {
            MONGO
                .getDatabase("global")
                .getCollection<Emote>("emotes")
                .find()
                .toList()
                .toMutableList()
        }
    }

    /** Get a communities emotes. */
    fun getCommunityEmotes(community: Community, includeGlobal: Boolean = true): List<Emote> =
        emoteCache.filter { emote ->
            (emote.parent == community.id) || (includeGlobal && emote.parent == -1L)
        }

    /** Get all global emotes. */
    fun getGlobalEmotes(): List<Emote> = emoteCache.filter { emote -> emote.parent == -1L }

    /**
     * Create an emote.
     *
     * @param name The name of the emote.
     * @param parent The parent community (or -1 if global).
     * @param createdBy The user's ID who created it.
     * @param image The actual emote.
     */
    @Throws(InvalidArguments::class, AlreadyExists::class, LimitReached::class)
    suspend fun createEmote(name: String, parent: Long, createdBy: Long, image: ByteArray) {
        val parsedName = cleanInput(name)

        when {
            parsedName.isBlank() || !EMOTE_NAME_REGEX.matches(name) ->
                throw InvalidArguments("name")
            emoteAlreadyExists(name, parent) -> throw AlreadyExists("emote", name)
            parent != -1L &&
                getCommunityEmotes(CommunityManager.getCommunityById(parent), false).size >=
                    MAX_EMOTE_PER_COMMUNITY -> throw LimitReached()
        }

        val emoteId = IdGenerator.getId()

        upload(parent, emoteId, image)

        val time = System.currentTimeMillis()

        MONGO
            .getDatabase("global")
            .getCollection<Emote>("emotes")
            .insertOne(Emote(emoteId, parent, name, createdBy, time))

        emoteCache.add(Emote(emoteId, parent, name, createdBy, time))
    }

    /** If an emote with this [name] already exists. */
    private fun emoteAlreadyExists(name: String, parent: Long) =
        emoteCache.any { emote -> emote.name.equals(name, true) && emote.parent == parent }

    /** Upload an emote for [parent]. */
    private fun upload(parent: Long, emoteId: Long, bytes: ByteArray): Long {
        S3ImageHandler.upload("emotes/${parent}.${emoteId}.jpg", bytes)

        emoteCache

        return emoteId
    }
}
