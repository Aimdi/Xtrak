package com.github.andreyasadchy.xtra.util.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KickPusherChatWebSocketTest {

    @Test
    fun buildChannelNames_includesChatroomAndOptionalChannel() {
        assertEquals(
            listOf("chatrooms.42.v2"),
            KickPusherChatWebSocket.buildChannelNames("42", null),
        )
        assertEquals(
            listOf("chatrooms.42.v2", "channel.99", "predictions-channel-99"),
            KickPusherChatWebSocket.buildChannelNames("42", "99"),
        )
        assertEquals(
            listOf("chatrooms.42.v2"),
            KickPusherChatWebSocket.buildChannelNames("42", "  "),
        )
    }

    @Test
    fun buildSocketUrl_usesKnownPusherApp() {
        val url = KickPusherChatWebSocket.buildSocketUrl()
        assertTrue(url.startsWith("wss://ws-us2.pusher.com/app/32cbd69e4b950bf97679"))
        assertTrue(url.contains("protocol=7"))
    }
}
