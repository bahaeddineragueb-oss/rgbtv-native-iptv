package com.rgbtv.app

import com.rgbtv.app.data.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the M3U parser.
 *
 * The original implementation dropped every entry whose #EXTINF line carried no attributes
 * (`#EXTINF:-1,Channel`) and mis-split names that contain commas, so most simple playlists
 * produced an empty or truncated list. These tests lock the corrected behaviour.
 */
class M3uParserTest {

    @Test
    fun parsesEntriesWithoutAttributes() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1,Al Jazeera
            http://example.com/aljazeera.ts
        """.trimIndent()

        val channels = M3uParser.parse(playlist)

        assertEquals(1, channels.size)
        assertEquals("Al Jazeera", channels[0].name)
        assertEquals("General", channels[0].group)
        assertEquals("http://example.com/aljazeera.ts", channels[0].streamUrl)
    }

    @Test
    fun keepsCommasInsideChannelNames() {
        val playlist = """
            #EXTINF:-1 tvg-logo="news.png" group-title="News",News, Live 24/7
            http://example.com/news.ts
        """.trimIndent()

        val channels = M3uParser.parse(playlist)

        assertEquals(1, channels.size)
        assertEquals("News, Live 24/7", channels[0].name)
        assertEquals("News", channels[0].group)
        assertEquals("news.png", channels[0].logo)
    }

    @Test
    fun readsAttributesAndGroups() {
        val playlist = """
            #EXTINF:-1 tvg-id="bein1.us" tvg-name="beIN Sports 1" tvg-logo="http://logo/bein.png" group-title="Sports",beIN Sports
            http://example.com/bein.m3u8
        """.trimIndent()

        val channels = M3uParser.parse(playlist)

        assertEquals("Sports", channels[0].group)
        assertEquals("http://logo/bein.png", channels[0].logo)
        assertEquals("beIN Sports", channels[0].name)
    }

    @Test
    fun supportsExtGrpAndBareUrls() {
        val playlist = """
            #EXTM3U
            #EXTGRP:Movies
            #EXTINF:-1,Cinema One
            http://example.com/cinema.m3u8
            http://example.com/loose-stream.m3u8
        """.trimIndent()

        val channels = M3uParser.parse(playlist)

        assertEquals(2, channels.size)
        assertEquals("Movies", channels[0].group)
        assertEquals("Cinema One", channels[0].name)
        // A URL without metadata still becomes a channel, named after the file.
        assertEquals("loose-stream", channels[1].name)
    }

    @Test
    fun ignoresCommentsAndBlankLines() {
        val playlist = """
            #EXTM3U x-tvg-url="http://epg.example.com"

            #EXTINF:-1,One
            http://example.com/one.ts

            #EXTINF:-1,Two
            http://example.com/two.ts
        """.trimIndent()

        val channels = M3uParser.parse(playlist)

        assertEquals(2, channels.size)
        assertTrue(channels.all { it.streamUrl.startsWith("http://") })
    }

    @Test
    fun producesUniqueIds() {
        val playlist = """
            #EXTINF:-1,One
            http://example.com/one.ts
            #EXTINF:-1,Two
            http://example.com/two.ts
        """.trimIndent()

        val ids = M3uParser.parse(playlist).map { it.id }

        assertEquals(2, ids.distinct().size)
    }
}
