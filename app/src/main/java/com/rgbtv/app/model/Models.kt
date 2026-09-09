package com.rgbtv.app.model

data class Channel(
    val id: String,
    val name: String,
    val group: String = "Live",
    val logo: String? = null,
    val streamUrl: String,
    val isFavorite: Boolean = false
)

enum class SourceType { M3U, XTREAM, STALKER }

data class Profile(
    val id: String,
    val name: String,
    val sourceType: SourceType,
    val endpoint: String,
    /** Username (Xtream) or MAC address (Stalker). Secrets never live here: they are encrypted. */
    val account: String? = null
)

data class PlayerState(
    val selectedChannel: Channel? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val error: String? = null
)

/**
 * A user-facing message that is resolved from a string resource by the UI layer, so the
 * ViewModel never holds (or leaks) provider URLs and credentials inside raw text.
 */
data class UiMessage(
    val resId: Int,
    val args: List<Any> = emptyList()
)
