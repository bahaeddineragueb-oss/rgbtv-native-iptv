package com.rgbtv.app.data

import com.rgbtv.app.model.Channel

object M3uParser {
    fun parse(text: String, sourceId: String = "m3u"): List<Channel> {
        require(text.length <= 50 * 1024 * 1024) { "Playlist is too large" }
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        val result = mutableListOf<Channel>()
        var metadata: Map<String, String> = emptyMap()
        var name = "Unnamed"
        for (line in lines) {
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val comma = line.lastIndexOf(',')
                    name = if (comma >= 0) line.substring(comma + 1).trim() else "Unnamed"
                    metadata = Regex("([\\w-]+)=\\\"([^\\\"]*)\\\"")
                        .findAll(line).associate { it.groupValues[1] to it.groupValues[2] }
                }
                !line.startsWith("#") && metadata.isNotEmpty() -> {
                    val url = line
                    result += Channel(
                        id = "$sourceId:${url.hashCode()}:${result.size}",
                        name = name,
                        group = metadata["group-title"].orEmpty().ifBlank { "General" },
                        logo = metadata["tvg-logo"]?.ifBlank { null },
                        streamUrl = url
                    )
                    metadata = emptyMap()
                }
            }
        }
        return result
    }
}
