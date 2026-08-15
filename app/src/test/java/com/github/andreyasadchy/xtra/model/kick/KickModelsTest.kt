package com.github.andreyasadchy.xtra.model.kick

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class KickModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decodeChannelResponse() {
        val channel = json.decodeFromString<KickChannelResponse>(
            """
            {
              "id": 123,
              "slug": "xqc",
              "playback_url": "https://stream.kick.com/hls/xqc",
              "followers_count": 10,
              "chatroom": { "id": 77 },
              "livestream": {
                "id": 9,
                "session_title": "live now",
                "viewer_count": 42,
                "playback_url": "https://stream.kick.com/hls/xqc/playlist.m3u8"
              },
              "user": { "username": "xQc", "profilepic": "https://images.kick.com/xqc.png" }
            }
            """.trimIndent()
        )
        assertEquals(123L, channel.id)
        assertEquals("xqc", channel.slug)
        assertEquals(77L, channel.chatroom?.id)
        assertEquals("live now", channel.livestream?.title)
        assertEquals("xQc", channel.user?.username)
        assertEquals("https://images.kick.com/xqc.png", channel.user?.profileImage)
    }

    @Test
    fun decodeChatMessageEvent() {
        val event = json.decodeFromString<KickChatMessageEvent>(
            """
            {
              "id": "msg-1",
              "content": "hello kick",
              "sender": {
                "id": 5,
                "slug": "viewer",
                "username": "Viewer",
                "identity": { "color": "#ff0000" }
              }
            }
            """.trimIndent()
        )
        assertEquals("msg-1", event.id)
        assertEquals("hello kick", event.content)
        assertEquals("Viewer", event.sender?.username)
        assertEquals("#ff0000", event.sender?.identity?.color)
    }

    @Test
    fun decodeWebsiteSearchResponse() {
        val response = json.decodeFromString<KickWebsiteSearchResponse>(
            """
            {
              "channels": [
                { "id": 1, "slug": "xqc", "is_live": true, "user": { "username": "xQc" } }
              ],
              "categories": [
                { "id": 2, "name": "Just Chatting", "slug": "just-chatting" }
              ],
              "livestreams": {
                "tags": [
                  { "id": 3, "session_title": "live", "channel": { "slug": "xqc" } }
                ]
              }
            }
            """.trimIndent()
        )
        assertEquals("xqc", response.channels.single().slug)
        assertEquals(true, response.channels.single().isLive)
        assertEquals("just-chatting", response.categories.single().slug)
        assertEquals("xqc", response.livestreams.tags.single().channel?.slug)
        assertNotNull(response.livestreams)
    }

    @Test
    fun decodeOfficialLivestreamList() {
        val response = json.decodeFromString<KickOfficialListResponse<KickOfficialLivestream>>(
            """
            {
              "data": [
                {
                  "broadcaster_user_id": 11,
                  "channel_id": 22,
                  "slug": "xqc",
                  "stream_title": "live on kick",
                  "viewer_count": 99,
                  "profile_picture": "https://images.kick.com/xqc.png",
                  "thumbnail": "https://images.kick.com/live.png",
                  "started_at": "2026-08-15T03:00:00Z",
                  "category": { "id": 8, "name": "Just Chatting" },
                  "custom_tags": ["irl"]
                }
              ],
              "message": "OK"
            }
            """.trimIndent()
        )
        val item = response.data.single()
        assertEquals("xqc", item.slug)
        assertEquals(99, item.viewerCount)
        assertEquals("Just Chatting", item.category?.name)
        assertEquals("OK", response.message)
    }

    @Test
    fun decodeFeaturedAndCategoriesTop() {
        val featured = json.decodeFromString<KickFeaturedLivestreamsResponse>(
            """{ "featured": [ { "id": 3, "session_title": "live", "channel": { "slug": "xqc" } } ] }"""
        )
        assertEquals("xqc", featured.items.single().channel?.slug)
        val categories = json.decodeFromString<KickCategoriesTopResponse>(
            """{ "data": [ { "id": 8, "name": "Just Chatting", "slug": "just-chatting" } ] }"""
        )
        assertEquals("just-chatting", categories.items.single().slug)
    }
}
