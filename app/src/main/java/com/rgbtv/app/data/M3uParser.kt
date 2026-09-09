package com.rgbtv.app.data

import com.rgbtv.app.model.Channel

/**
 * Minimal, forgiving M3U parser.
 *
 * Important: most real playlists contain entries such as `#EXTINF:-1,Al Jazeera` with no
 * attributes at all, and names frequently contain commas (`News, Live`). The parser therefore
 * tracks "we just saw an #EXTINF" separately from the attribute map, and splits the display
 * name on the first comma that is **not** inside quotes.
 */
object M3uParser {

    /** Playlists above this size are rejected before parsing to protect memory. */
    const val MAX_PLAYLIST_CHARS = 25 * 1024 * 1024

    private const val DEFAULT_GROUP = "General"
    private const val FALLBACK_NAME = "Channel"

    private val ATTRIBUTES = Regex("([\\w-]+)\\s*=\\s*\"([^\"]*)\"")

    fun parse(text: String, sourceId: String = "m3u"): List<Channel> {
        val result = ArrayList<Channel>(512)
        var pending: Entry? = null

        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach

            if (line.startsWith("#")) {
                when {
                    line.startsWith("#EXTINF", ignoreCase = true) -> {
                        // #EXTGRP can appear before or after #EXTINF, so keep a group that was
                        // already declared when this entry does not carry group-title itself.
                        val parsed = parseExtInf(line)
                        val declaredGroup = pending?.group.orEmpty()
                        pending = if (parsed.group.isBlank() && declaredGroup.isNotBlank()) {
                            parsed.copy(group = declaredGroup)
                        } else {
                            parsed
                        }
                    }

                    line.startsWith("#EXTGRP", ignoreCase = true) -> {
                        val group = line.substringAfter(':').trim()
                        if (group.isNotEmpty()) pending = (pending ?: Entry()).copy(group = group)
                    }
                }
                return@forEach
            }

            val info = pending ?: Entry()
            pending = null
            val name = info.name.ifBlank { info.tvgName }.ifBlank { deriveName(line) }
            result += Channel(
                id = "$sourceId:${result.size}:${line.hashCode()}",
                name = name,
                group = info.group.ifBlank { DEFAULT_GROUP },
                logo = info.logo,
                streamUrl = line
            )
        }

        return result
    }

    private fun parseExtInf(line: String): Entry {
        val payload = line.substringAfter(':').trim()
        val attributes = ATTRIBUTES.findAll(payload)
            .associate { it.groupValues[1].lowercase() to it.groupValues[2] }
        val comma = firstUnquotedComma(payload)
        val name = if (comma >= 0) payload.substring(comma + 1).trim() else ""

        return Entry(
            name = name,
            tvgName = attributes["tvg-name"].orEmpty(),
            group = attributes["group-title"].orEmpty(),
            logo = attributes["tvg-logo"]?.ifBlank { null } ?: attributes["logo"]?.ifBlank { null }
        )
    }

    /** Index of the comma that separates attributes from the channel name. */
    private fun firstUnquotedComma(value: String): Int {
        var inQuotes = false
        value.forEachIndexed { index, char ->
            when (char) {
                '"' -> inQuotes = !inQuotes
                ',' -> if (!inQuotes) return index
            }
        }
        return -1
    }

    private fun deriveName(url: String): String {
        val file = url.substringAfterLast('/').substringBefore('?').substringBefore('#')
        val withoutExtension = file.substringBeforeLast('.')
        val candidate = if (withoutExtension.isBlank()) file else withoutExtension
        return candidate.ifBlank { FALLBACK_NAME }
    }

    private data class Entry(
        val name: String = "",
        val tvgName: String = "",
        val group: String = "",
        val logo: String? = null
    )
}
