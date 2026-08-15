package com.github.andreyasadchy.xtra.repository

import com.github.andreyasadchy.xtra.model.kick.KickChannelLivestream
import com.github.andreyasadchy.xtra.model.kick.KickChannelLivestreamResponse
import com.github.andreyasadchy.xtra.model.kick.KickChannelResponse
import com.github.andreyasadchy.xtra.model.kick.KickLivestreamsResponse
import com.github.andreyasadchy.xtra.model.kick.KickSubcategoriesResponse
import com.github.andreyasadchy.xtra.model.kick.KickWebsiteSearchResponse
import com.github.andreyasadchy.xtra.model.ui.Game
import com.github.andreyasadchy.xtra.model.ui.Stream
import com.github.andreyasadchy.xtra.model.ui.User
import com.github.andreyasadchy.xtra.util.KickApiHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.util.Locale

class KickRepository(
    private val okHttpClient: Lazy<OkHttpClient>,
    private val json: Json,
) {
    suspend fun getChannel(channelSlug: String): KickChannelResponse = withContext(Dispatchers.IO) {
        json.decodeFromString(getRaw("https://kick.com/api/v2/channels/${urlEncode(channelSlug)}"))
    }

    suspend fun getChannelLivestream(channelSlug: String): KickChannelLivestream? = withContext(Dispatchers.IO) {
        val raw = getRaw("https://kick.com/api/v2/channels/${urlEncode(channelSlug)}/livestream")
        runCatching { json.decodeFromString<KickChannelLivestreamResponse>(raw).data }.getOrNull()
            ?: runCatching { json.decodeFromString<KickChannelLivestream>(raw) }.getOrNull()
    }

    suspend fun getPlayableUrl(channelSlug: String): String {
        val channel = getChannel(channelSlug)
        val livestream = channel.livestream ?: runCatching { getChannelLivestream(channelSlug) }.getOrNull()
        return KickApiHelper.normalizePlaybackUrl(livestream?.playbackUrl ?: channel.playbackUrl)
            ?: throw IOException("Kick channel $channelSlug is offline or has no playback URL")
    }

    suspend fun searchWebsite(query: String): KickWebsiteSearchResponse = withContext(Dispatchers.IO) {
        json.decodeFromString(getRaw(KickWebsiteSearchRequest.buildUrl(query)))
    }

    suspend fun getLivestreams(page: Int, limit: Int, subcategory: String? = null): KickLivestreamsResponse = withContext(Dispatchers.IO) {
        val url = "https://kick.com/stream/livestreams/en".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("limit", limit.toString())
            .apply {
                subcategory?.takeIf { it.isNotBlank() }?.let { addQueryParameter("subcategory", it) }
            }
            .build()
        json.decodeFromString(getRaw(url.toString()))
    }

    suspend fun getSubcategories(page: Int, limit: Int): KickSubcategoriesResponse = withContext(Dispatchers.IO) {
        val url = "https://kick.com/api/v1/subcategories".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("limit", limit.toString())
            .build()
        json.decodeFromString(getRaw(url.toString()))
    }

    suspend fun searchStreams(query: String): List<Stream> {
        if (query.trim().length < KickWebsiteSearchRequest.MIN_QUERY_LENGTH) return emptyList()
        val response = searchWebsite(query)
        val livestreams = response.livestreams.tags.map(KickWebsiteSearchMapper::toStream)
        val liveChannels = response.channels.filter { it.isLive == true }.map { channel ->
            KickWebsiteSearchMapper.toStream(
                channel = channel,
                livestream = KickChannelLivestream(
                    playbackUrl = channel.playbackUrl,
                    title = channel.user?.username,
                )
            )
        }
        return (livestreams + liveChannels).distinctBy { it.channelLogin?.lowercase(Locale.ROOT) }
    }

    suspend fun searchChannels(query: String): List<User> {
        if (query.trim().length < KickWebsiteSearchRequest.MIN_QUERY_LENGTH) return emptyList()
        return searchWebsite(query).channels.map(KickWebsiteSearchMapper::toUser)
    }

    suspend fun searchGames(query: String): List<Game> {
        if (query.trim().length < KickWebsiteSearchRequest.MIN_QUERY_LENGTH) return emptyList()
        return searchWebsite(query).categories.map(KickWebsiteSearchMapper::toGame)
    }

    suspend fun loadUser(login: String): User? {
        return runCatching { KickWebsiteSearchMapper.toUser(getChannel(login)) }.getOrNull()
    }

    suspend fun loadStream(login: String): Stream? {
        return runCatching {
            val channel = getChannel(login)
            KickWebsiteSearchMapper.toStream(channel).takeIf { channel.livestream != null || !channel.playbackUrl.isNullOrBlank() }
        }.getOrNull()
    }

    fun toStream(channel: KickChannelResponse): Stream = KickWebsiteSearchMapper.toStream(channel)

    fun toUser(channel: KickChannelResponse): User = KickWebsiteSearchMapper.toUser(channel)

    private fun getRaw(url: String): String {
        val request = Request.Builder().url(url).apply {
            KickApiHelper.webHeaders().forEach { (key, value) -> header(key, value) }
        }.build()
        return okHttpClient.value.newCall(request).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) {
                throw IOException("Kick request failed (${response.code}) for $url: ${body.take(200)}")
            }
            body
        }
    }

    private fun urlEncode(value: String): String {
        return URLEncoder.encode(value.trim(), Charsets.UTF_8.name())
    }
}
