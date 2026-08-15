package com.github.andreyasadchy.xtra.repository

import android.annotation.SuppressLint
import android.content.Context
import android.net.http.HttpEngine
import android.util.Log
import android.webkit.CookieManager
import androidx.core.content.edit
import com.github.andreyasadchy.xtra.model.kick.KickAppTokenResponse
import com.github.andreyasadchy.xtra.model.kick.KickCategoriesTopResponse
import com.github.andreyasadchy.xtra.model.kick.KickChannelLivestream
import com.github.andreyasadchy.xtra.model.kick.KickChannelLivestreamResponse
import com.github.andreyasadchy.xtra.model.kick.KickChannelResponse
import com.github.andreyasadchy.xtra.model.kick.KickFeaturedLivestreamsResponse
import com.github.andreyasadchy.xtra.model.kick.KickLivestream
import com.github.andreyasadchy.xtra.model.kick.KickLivestreamsResponse
import com.github.andreyasadchy.xtra.model.kick.KickOfficialCategory
import com.github.andreyasadchy.xtra.model.kick.KickOfficialChannel
import com.github.andreyasadchy.xtra.model.kick.KickOfficialListResponse
import com.github.andreyasadchy.xtra.model.kick.KickOfficialLivestream
import com.github.andreyasadchy.xtra.model.kick.KickSubcategoriesResponse
import com.github.andreyasadchy.xtra.model.kick.KickWebsiteSearchResponse
import com.github.andreyasadchy.xtra.model.ui.Game
import com.github.andreyasadchy.xtra.model.ui.Stream
import com.github.andreyasadchy.xtra.model.ui.User
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.KickApiHelper
import com.github.andreyasadchy.xtra.util.NetworkUtils
import com.github.andreyasadchy.xtra.util.prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.chromium.net.CronetEngine
import java.io.IOException
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService

class KickRepository(
    private val applicationContext: Context,
    private val httpEngine: Lazy<HttpEngine?>,
    private val cronetEngine: Lazy<CronetEngine?>,
    private val cronetExecutor: Lazy<ExecutorService>,
    private val okHttpClient: Lazy<OkHttpClient>,
    private val json: Json,
) {
    private val tag = "KickRepository"
    private val sessionCookies = ConcurrentHashMap<String, String>()
    private val sessionLock = Mutex()
    @Volatile private var sessionReady = false

    suspend fun getChannel(channelSlug: String): KickChannelResponse = withContext(Dispatchers.IO) {
        json.decodeFromString(getWebsiteRaw("https://kick.com/api/v2/channels/${urlEncode(channelSlug)}"))
    }

    suspend fun getChannelLivestream(channelSlug: String): KickChannelLivestream? = withContext(Dispatchers.IO) {
        val raw = getWebsiteRaw("https://kick.com/api/v2/channels/${urlEncode(channelSlug)}/livestream")
        runCatching { json.decodeFromString<KickChannelLivestreamResponse>(raw).data }.getOrNull()
            ?: runCatching { json.decodeFromString<KickChannelLivestream>(raw) }.getOrNull()
    }

    suspend fun getPlayableUrl(channelSlug: String): String {
        runCatching {
            val channel = getChannel(channelSlug)
            val livestream = channel.livestream ?: runCatching { getChannelLivestream(channelSlug) }.getOrNull()
            KickApiHelper.normalizePlaybackUrl(livestream?.playbackUrl ?: channel.playbackUrl)
        }.getOrNull()?.let { return it }
        val official = getOfficialChannel(channelSlug)
        return KickApiHelper.normalizePlaybackUrl(official?.stream?.url)
            ?: throw IOException("Kick channel $channelSlug is offline or has no playback URL")
    }

    suspend fun searchWebsite(query: String): KickWebsiteSearchResponse = withContext(Dispatchers.IO) {
        json.decodeFromString(getWebsiteRaw(KickWebsiteSearchRequest.buildUrl(query)))
    }

    suspend fun getLivestreams(page: Int, limit: Int, subcategory: String? = null): KickLivestreamsResponse = withContext(Dispatchers.IO) {
        val url = "https://kick.com/stream/livestreams/en".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("limit", limit.toString())
            .addQueryParameter("sort", "desc")
            .apply {
                subcategory?.takeIf { it.isNotBlank() }?.let { addQueryParameter("subcategory", it) }
            }
            .build()
        json.decodeFromString(getWebsiteRaw(url.toString()))
    }

    suspend fun getSubcategories(page: Int, limit: Int): KickSubcategoriesResponse = withContext(Dispatchers.IO) {
        val url = "https://kick.com/api/v1/subcategories".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("limit", limit.toString())
            .build()
        json.decodeFromString(getWebsiteRaw(url.toString()))
    }

    suspend fun loadTopStreams(page: Int, limit: Int, subcategory: String? = null): List<Stream> {
        val website = runCatching {
            getLivestreams(page, limit, subcategory).data.map(KickWebsiteSearchMapper::toStream)
        }.onFailure { Log.w(tag, "website livestreams failed: ${it.message}") }.getOrDefault(emptyList())
        if (website.isNotEmpty()) return website
        if (subcategory.isNullOrBlank()) {
            val featured = runCatching {
                decodeFeatured(getWebsiteRaw("https://kick.com/api/v2/featured-livestreams/en?limit=$limit"))
                    .map(KickWebsiteSearchMapper::toStream)
            }.onFailure { Log.w(tag, "featured livestreams failed: ${it.message}") }.getOrDefault(emptyList())
            if (featured.isNotEmpty()) return featured
        }
        return loadOfficialLivestreams(limit, subcategory)
    }

    suspend fun loadTopGames(page: Int, limit: Int): List<Game> {
        val website = runCatching {
            getSubcategories(page, limit).data.map(KickWebsiteSearchMapper::toGame)
        }.onFailure { Log.w(tag, "website subcategories failed: ${it.message}") }.getOrDefault(emptyList())
        if (website.isNotEmpty()) return website
        val top = runCatching {
            json.decodeFromString<KickCategoriesTopResponse>(
                getWebsiteRaw("https://kick.com/api/v1/categories/top?limit=$limit")
            ).items.map(KickWebsiteSearchMapper::toGame)
        }.onFailure { Log.w(tag, "website categories/top failed: ${it.message}") }.getOrDefault(emptyList())
        if (top.isNotEmpty()) return top
        return loadOfficialCategories("a", page)
    }

    suspend fun searchStreams(query: String): List<Stream> {
        if (query.trim().length < KickWebsiteSearchRequest.MIN_QUERY_LENGTH) return emptyList()
        val website = runCatching {
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
            (livestreams + liveChannels).distinctBy { it.channelLogin?.lowercase(Locale.ROOT) }
        }.onFailure { Log.w(tag, "website stream search failed: ${it.message}") }.getOrDefault(emptyList())
        if (website.isNotEmpty()) return website
        val officialChannel = runCatching { getOfficialChannel(query.trim()) }.getOrNull()
        if (officialChannel?.stream?.isLive == true) {
            return listOf(KickWebsiteSearchMapper.toStream(officialChannel))
        }
        val needle = query.trim().lowercase(Locale.ROOT)
        return loadOfficialLivestreams(50, null).filter { stream ->
            stream.channelLogin?.contains(needle, true) == true ||
                stream.channelName?.contains(needle, true) == true ||
                stream.title?.contains(needle, true) == true
        }
    }

    suspend fun searchChannels(query: String): List<User> {
        if (query.trim().length < KickWebsiteSearchRequest.MIN_QUERY_LENGTH) return emptyList()
        val website = runCatching {
            searchWebsite(query).channels.map(KickWebsiteSearchMapper::toUser)
        }.onFailure { Log.w(tag, "website channel search failed: ${it.message}") }.getOrDefault(emptyList())
        if (website.isNotEmpty()) return website
        return runCatching { getOfficialChannel(query.trim()) }.getOrNull()
            ?.let { listOf(KickWebsiteSearchMapper.toUser(it)) }
            .orEmpty()
    }

    suspend fun searchGames(query: String): List<Game> {
        if (query.trim().length < KickWebsiteSearchRequest.MIN_QUERY_LENGTH) return emptyList()
        val website = runCatching {
            searchWebsite(query).categories.map(KickWebsiteSearchMapper::toGame)
        }.onFailure { Log.w(tag, "website game search failed: ${it.message}") }.getOrDefault(emptyList())
        if (website.isNotEmpty()) return website
        return loadOfficialCategories(query.trim(), 1)
    }

    suspend fun loadUser(login: String): User? {
        return runCatching { KickWebsiteSearchMapper.toUser(getChannel(login)) }.getOrNull()
            ?: runCatching { getOfficialChannel(login)?.let(KickWebsiteSearchMapper::toUser) }.getOrNull()
    }

    suspend fun loadStream(login: String): Stream? {
        return runCatching {
            val channel = getChannel(login)
            KickWebsiteSearchMapper.toStream(channel).takeIf { channel.livestream != null || !channel.playbackUrl.isNullOrBlank() }
        }.getOrNull()
            ?: runCatching {
                getOfficialChannel(login)?.takeIf { it.stream?.isLive == true }?.let(KickWebsiteSearchMapper::toStream)
            }.getOrNull()
    }

    fun toStream(channel: KickChannelResponse): Stream = KickWebsiteSearchMapper.toStream(channel)

    fun toUser(channel: KickChannelResponse): User = KickWebsiteSearchMapper.toUser(channel)

    private suspend fun loadOfficialLivestreams(limit: Int, subcategory: String?): List<Stream> {
        val token = getAppAccessToken() ?: return emptyList()
        val categoryId = resolveOfficialCategoryId(subcategory)
        if (!subcategory.isNullOrBlank() && categoryId == null) {
            return emptyList()
        }
        val url = "${KickApiHelper.OFFICIAL_API}/livestreams".toHttpUrl().newBuilder()
            .addQueryParameter("limit", limit.coerceIn(1, 100).toString())
            .addQueryParameter("sort", "viewer_count")
            .apply {
                categoryId?.let { addQueryParameter("category_id", it.toString()) }
            }
            .build()
        return runCatching {
            json.decodeFromString<KickOfficialListResponse<KickOfficialLivestream>>(
                getOfficialRaw(url.toString(), token)
            ).data.map(KickWebsiteSearchMapper::toStream)
        }.onFailure { Log.w(tag, "official livestreams failed: ${it.message}") }.getOrDefault(emptyList())
    }

    private suspend fun resolveOfficialCategoryId(subcategory: String?): Long? {
        val raw = subcategory?.trim()?.takeIf { it.isNotBlank() } ?: return null
        raw.toLongOrNull()?.let { return it }
        return loadOfficialCategories(raw, 1).firstOrNull { game ->
            game.slug.equals(raw, ignoreCase = true) || game.name.equals(raw, ignoreCase = true)
        }?.id?.toLongOrNull()
    }

    private fun decodeFeatured(raw: String): List<KickLivestream> {
        runCatching { json.decodeFromString<KickFeaturedLivestreamsResponse>(raw).items }
            .getOrNull()?.takeIf { it.isNotEmpty() }?.let { return it }
        return runCatching { json.decodeFromString<KickLivestreamsResponse>(raw).data }.getOrDefault(emptyList())
    }

    private suspend fun loadOfficialCategories(query: String, page: Int): List<Game> {
        val token = getAppAccessToken() ?: return emptyList()
        val url = "${KickApiHelper.OFFICIAL_API}/categories".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("page", page.toString())
            .build()
        return runCatching {
            json.decodeFromString<KickOfficialListResponse<KickOfficialCategory>>(
                getOfficialRaw(url.toString(), token)
            ).data.map(KickWebsiteSearchMapper::toGame)
        }.onFailure { Log.w(tag, "official categories failed: ${it.message}") }.getOrDefault(emptyList())
    }

    private suspend fun getOfficialChannel(slug: String): KickOfficialChannel? {
        val token = getAppAccessToken() ?: return null
        val url = "${KickApiHelper.OFFICIAL_API}/channels".toHttpUrl().newBuilder()
            .addQueryParameter("slug", slug)
            .build()
        return runCatching {
            json.decodeFromString<KickOfficialListResponse<KickOfficialChannel>>(
                getOfficialRaw(url.toString(), token)
            ).data.firstOrNull()
        }.onFailure { Log.w(tag, "official channel failed: ${it.message}") }.getOrNull()
    }

    private suspend fun getAppAccessToken(): String? {
        val prefs = applicationContext.prefs()
        val cached = prefs.getString(C.KICK_APP_TOKEN, null)
        val expiry = prefs.getLong(C.KICK_APP_TOKEN_EXPIRY, 0L)
        if (!cached.isNullOrBlank() && System.currentTimeMillis() < expiry) {
            return cached
        }
        val clientId = prefs.getString(C.KICK_CLIENT_ID, null)?.trim().orEmpty()
        val clientSecret = prefs.getString(C.KICK_CLIENT_SECRET, null)?.trim().orEmpty()
        if (clientId.isBlank() || clientSecret.isBlank()) {
            return null
        }
        return runCatching {
            val body = FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .build()
            val raw = getRaw(
                url = KickApiHelper.TOKEN_URL,
                headers = mapOf(
                    "Accept" to "application/json",
                    "User-Agent" to KickApiHelper.USER_AGENT,
                ),
                method = "POST",
                postBody = body,
                preferChromium = false,
            )
            val token = json.decodeFromString<KickAppTokenResponse>(raw)
            val accessToken = token.accessToken?.takeIf { it.isNotBlank() }
                ?: throw IOException("Kick app token response had no access_token")
            val expiresInMs = ((token.expiresIn ?: 3600L) - 60L).coerceAtLeast(60L) * 1000L
            prefs.edit {
                putString(C.KICK_APP_TOKEN, accessToken)
                putLong(C.KICK_APP_TOKEN_EXPIRY, System.currentTimeMillis() + expiresInMs)
            }
            accessToken
        }.onFailure { Log.w(tag, "Kick app token failed: ${it.message}") }.getOrNull()
    }

    private suspend fun getWebsiteRaw(url: String): String {
        ensureWebsiteSession()
        return getRaw(url, KickApiHelper.webHeaders(cookieHeader()), preferChromium = true)
    }

    private suspend fun ensureWebsiteSession() {
        if (sessionReady && KickApiHelper.xsrfToken(cookieHeader()) != null) return
        sessionLock.withLock {
            if (sessionReady && KickApiHelper.xsrfToken(cookieHeader()) != null) return
            runCatching {
                getRaw(
                    url = KickApiHelper.CSRF_URL,
                    headers = KickApiHelper.webHeaders(cookieHeader()),
                    preferChromium = true,
                )
            }.onFailure { Log.w(tag, "Kick CSRF bootstrap failed: ${it.message}") }
            if (KickApiHelper.xsrfToken(cookieHeader()) == null) {
                runCatching {
                    getRaw(
                        url = KickApiHelper.WEB_ORIGIN + "/",
                        headers = KickApiHelper.htmlHeaders(cookieHeader()),
                        preferChromium = true,
                    )
                }.onFailure { Log.w(tag, "Kick homepage bootstrap failed: ${it.message}") }
            }
            sessionReady = true
        }
    }

    private suspend fun getOfficialRaw(url: String, token: String): String {
        val clientId = applicationContext.prefs().getString(C.KICK_CLIENT_ID, null)
        return getRaw(url, KickApiHelper.officialHeaders(token, clientId), preferChromium = false)
    }

    private suspend fun getRaw(
        url: String,
        headers: Map<String, String>,
        method: String = "GET",
        postBody: FormBody? = null,
        preferChromium: Boolean,
    ): String = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        if (preferChromium && method == "GET") {
            runCatching { getWithHttpEngine(url, headers) }.onSuccess { return@withContext requireSuccess(url, it) }.onFailure { errors += "HttpEngine:${it.message}" }
            runCatching { getWithCronet(url, headers) }.onSuccess { return@withContext requireSuccess(url, it) }.onFailure { errors += "Cronet:${it.message}" }
            if (errors.isNotEmpty()) {
                Log.w(tag, "Chromium Kick fetch failed for $url: ${errors.joinToString()}")
            }
        }
        val raw = getWithOkHttp(url, headers, method, postBody)
        requireSuccess(url, raw)
    }

    private fun requireSuccess(url: String, body: String): String {
        if (KickApiHelper.isBlockedBody(body)) {
            throw IOException("Kick request blocked by Cloudflare for $url")
        }
        return body
    }

    @SuppressLint("NewApi")
    private suspend fun getWithHttpEngine(url: String, headers: Map<String, String>): String {
        val engine = httpEngine.value ?: throw IOException("HttpEngine unavailable")
        val response = suspendCancellableCoroutine { continuation ->
            val timeout = NetworkUtils.HttpEngineTimeout()
            val request = engine.newUrlRequestBuilder(
                url,
                cronetExecutor.value,
                NetworkUtils.ByteArrayUrlCallback(continuation, timeout)
            ).apply {
                headers.forEach { addHeader(it.key, it.value) }
            }.build()
            timeout.start(request, continuation)
            request.start()
            continuation.invokeOnCancellation {
                request.cancel()
                timeout.stop()
            }
        }
        rememberCookies(KickApiHelper.setCookieValues(response.info.headers.asMap))
        if (response.info.httpStatusCode !in 200..299) {
            throw IOException("Kick request failed (${response.info.httpStatusCode}) for $url: ${response.body.decodeToString().take(200)}")
        }
        return response.body.decodeToString()
    }

    private suspend fun getWithCronet(url: String, headers: Map<String, String>): String {
        val engine = cronetEngine.value ?: throw IOException("Cronet unavailable")
        val response = suspendCancellableCoroutine { continuation ->
            val timeout = NetworkUtils.CronetTimeout()
            val request = engine.newUrlRequestBuilder(
                url,
                NetworkUtils.ByteArrayCronetCallback(continuation, timeout),
                cronetExecutor.value
            ).apply {
                headers.forEach { addHeader(it.key, it.value) }
            }.build()
            timeout.start(request, continuation)
            request.start()
            continuation.invokeOnCancellation {
                request.cancel()
                timeout.stop()
            }
        }
        rememberCookies(KickApiHelper.setCookieValues(response.info.allHeaders))
        if (response.info.httpStatusCode !in 200..299) {
            throw IOException("Kick request failed (${response.info.httpStatusCode}) for $url: ${response.body.decodeToString().take(200)}")
        }
        return response.body.decodeToString()
    }

    private fun getWithOkHttp(url: String, headers: Map<String, String>, method: String, postBody: FormBody?): String {
        val request = Request.Builder().url(url).apply {
            headers.forEach { (key, value) -> header(key, value) }
            if (method == "POST") {
                post(postBody ?: FormBody.Builder().build())
            }
        }.build()
        return okHttpClient.value.newCall(request).execute().use { response ->
            rememberCookies(response.headers("Set-Cookie"))
            val body = response.body.string()
            if (!response.isSuccessful) {
                throw IOException("Kick request failed (${response.code}) for $url: ${body.take(200)}")
            }
            body
        }
    }

    private fun rememberCookies(setCookieHeaders: List<String>?) {
        setCookieHeaders.orEmpty().forEach { header ->
            KickApiHelper.parseSetCookie(header)?.let { (name, value) ->
                sessionCookies[name] = value
                runCatching {
                    CookieManager.getInstance().setCookie(KickApiHelper.WEB_ORIGIN, "$name=$value")
                }
            }
        }
    }

    private fun cookieHeader(): String? {
        val fromManager = runCatching {
            CookieManager.getInstance().getCookie(KickApiHelper.WEB_ORIGIN)?.takeIf { it.isNotBlank() }
                ?: CookieManager.getInstance().getCookie(KickApiHelper.WEB_ORIGIN_ALT)?.takeIf { it.isNotBlank() }
        }.getOrNull()
        return KickApiHelper.mergeCookieHeader(fromManager, KickApiHelper.cookieHeader(sessionCookies))
    }

    private fun urlEncode(value: String): String {
        return URLEncoder.encode(value.trim(), Charsets.UTF_8.name())
    }
}
