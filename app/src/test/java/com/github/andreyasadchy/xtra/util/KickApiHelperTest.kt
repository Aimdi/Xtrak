package com.github.andreyasadchy.xtra.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KickApiHelperTest {

    @Test
    fun isKickSource_matchesSourceOrPrefixedStreamId() {
        assertTrue(KickApiHelper.isKickSource(C.KICK, "123"))
        assertTrue(KickApiHelper.isKickSource("KICK", null))
        assertTrue(KickApiHelper.isKickSource(null, "kick:456"))
        assertFalse(KickApiHelper.isKickSource(C.TWITCH, "123"))
        assertFalse(KickApiHelper.isKickSource(null, null))
    }

    @Test
    fun kickStreamId_prefixesAndDeduplicates() {
        assertEquals("kick:123", KickApiHelper.kickStreamId("123"))
        assertEquals("kick:123", KickApiHelper.kickStreamId("kick:123"))
        assertEquals("kick:unknown", KickApiHelper.kickStreamId(null))
        assertEquals("kick:unknown", KickApiHelper.kickStreamId("   "))
    }

    @Test
    fun normalizePlaybackUrl_appendsPlaylistForKickHls() {
        assertEquals(
            "https://fa723fc1b171.playback.live-video.net/api/video/v1/x.m3u8",
            KickApiHelper.normalizePlaybackUrl("https://fa723fc1b171.playback.live-video.net/api/video/v1/x.m3u8"),
        )
        assertEquals(
            "https://stream.kick.com/hls/channel/playlist.m3u8",
            KickApiHelper.normalizePlaybackUrl("https://stream.kick.com/hls/channel"),
        )
        assertEquals(
            "https://stream.kick.com/stream/channel/playlist.m3u8",
            KickApiHelper.normalizePlaybackUrl("https://stream.kick.com/stream/channel/"),
        )
        assertNull(KickApiHelper.normalizePlaybackUrl("  "))
        assertNull(KickApiHelper.normalizePlaybackUrl(null))
    }

    @Test
    fun isReservedKickPath_blocksSitePages() {
        assertTrue(KickApiHelper.isReservedKickPath("search"))
        assertTrue(KickApiHelper.isReservedKickPath("Login"))
        assertFalse(KickApiHelper.isReservedKickPath("xqc"))
    }

    @Test
    fun channelShareUrl_usesKickHost() {
        assertEquals("https://kick.com/xqc", KickApiHelper.channelShareUrl("xqc"))
        assertEquals("https://kick.com/xqc", KickApiHelper.channelShareUrl("/xqc"))
    }

    @Test
    fun isKickCdnUrl_detectsKickHosts() {
        assertTrue(KickApiHelper.isKickCdnUrl("https://images.kick.com/user.png"))
        assertFalse(KickApiHelper.isKickCdnUrl("https://static-cdn.jtvnw.net/user.png"))
    }
}
