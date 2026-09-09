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
    val endpoint: String
)

data class PlayerState(
    val selectedChannel: Channel? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val error: String? = null
)
