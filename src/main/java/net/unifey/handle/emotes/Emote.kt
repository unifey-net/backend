package net.unifey.handle.emotes

import kotlinx.serialization.Serializable

/**
 * An emote.
 *
 * @param url The full image URL of the emote.
 * @param id The ID of the emote.
 * @param parent The parent community. If this is -1, it is a global emote
 * @param name The name of the emote.
 * @param date The date the emote was uploaded.
 */
@Serializable
data class Emote(
    val url: String,
    val id: Long,
    val parent: Long,
    val name: String,
    val uploadedBy: Long,
    val date: Long
)
