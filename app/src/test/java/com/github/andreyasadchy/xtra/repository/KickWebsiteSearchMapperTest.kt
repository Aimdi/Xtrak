package com.github.andreyasadchy.xtra.repository

import com.github.andreyasadchy.xtra.model.kick.KickOfficialCategory
import com.github.andreyasadchy.xtra.model.kick.KickOfficialChannel
import com.github.andreyasadchy.xtra.model.kick.KickOfficialChannelStream
import com.github.andreyasadchy.xtra.model.kick.KickOfficialLivestream
import com.github.andreyasadchy.xtra.util.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KickWebsiteSearchMapperTest {

    @Test
    fun officialLivestream_mapsKickSourceAndPrefixedId() {
        val stream = KickWebsiteSearchMapper.toStream(
            KickOfficialLivestream(
                broadcasterUserId = 11,
                category = KickOfficialCategory(id = 8, name = "Just Chatting"),
                channelId = 22,
                profilePicture = "https://images.kick.com/xqc.png",
                slug = "xqc",
                startedAt = "2026-08-15T03:00:00Z",
                streamTitle = "live on kick",
                thumbnail = "https://images.kick.com/live.png",
                viewerCount = 99,
                customTags = listOf("irl"),
            )
        )
        assertEquals("kick:22", stream.id)
        assertEquals("xqc", stream.channelLogin)
        assertEquals("live on kick", stream.title)
        assertEquals(C.KICK, stream.source)
        assertTrue(stream.isKick)
    }

    @Test
    fun officialChannel_mapsLiveUser() {
        val user = KickWebsiteSearchMapper.toUser(
            KickOfficialChannel(
                broadcasterUserId = 11,
                slug = "xqc",
                stream = KickOfficialChannelStream(isLive = true, viewerCount = 10),
                streamTitle = "live",
                thumbnail = "https://images.kick.com/xqc.png",
            )
        )
        assertEquals("xqc", user.login)
        assertEquals(true, user.isLive)
        assertEquals(C.KICK, user.source)
        assertTrue(user.isKick)
    }
}
