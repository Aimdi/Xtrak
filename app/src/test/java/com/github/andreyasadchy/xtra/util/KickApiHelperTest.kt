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

    @Test
    fun isBlockedBody_detectsCloudflarePolicy() {
        assertTrue(KickApiHelper.isBlockedBody("""{"error":"Request blocked by security policy."}"""))
        assertFalse(KickApiHelper.isBlockedBody("""{"data":[]}"""))
    }

    @Test
    fun labeledName_appendsKick() {
        assertEquals("xQc · Kick", KickApiHelper.labeledName("xQc"))
        assertNull(KickApiHelper.labeledName(null))
    }

    @Test
    fun parseSetCookie_readsNameAndValue() {
        val cookie = KickApiHelper.parseSetCookie("XSRF-TOKEN=abc%3D%3D; Path=/; Secure; HttpOnly")
        assertEquals("XSRF-TOKEN", cookie?.first)
        assertEquals("abc%3D%3D", cookie?.second)
        assertNull(KickApiHelper.parseSetCookie("Path=/"))
    }

    @Test
    fun xsrfToken_urlDecodesCookie() {
        val header = "kick_session=one; XSRF-TOKEN=abc%3D%3D; other=two"
        assertEquals("abc==", KickApiHelper.xsrfToken(header))
        assertEquals("X-XSRF-TOKEN", KickApiHelper.webHeaders(header).keys.single { it.equals("X-XSRF-TOKEN", true) })
        assertEquals("abc==", KickApiHelper.webHeaders(header)["X-XSRF-TOKEN"])
    }

    @Test
    fun mergeCookieHeader_prefersLaterValues() {
        assertEquals(
            "a=1; b=3",
            KickApiHelper.mergeCookieHeader("a=1; b=2", "b=3"),
        )
    }

    @Test
    fun setCookieValues_isCaseInsensitive() {
        val headers = mapOf("set-cookie" to listOf("XSRF-TOKEN=token", "kick_session=sess"))
        assertEquals(listOf("XSRF-TOKEN=token", "kick_session=sess"), KickApiHelper.setCookieValues(headers))
    }
}
