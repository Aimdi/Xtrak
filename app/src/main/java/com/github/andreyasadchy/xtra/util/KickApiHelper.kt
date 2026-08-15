package com.github.andreyasadchy.xtra.util

import java.net.URLDecoder

object KickApiHelper {
    const val STREAM_ID_PREFIX = "kick:"
    const val WEB_ORIGIN = "https://kick.com"
    const val WEB_ORIGIN_ALT = "https://web.kick.com"
    const val CSRF_URL = "https://kick.com/sanctum/csrf-cookie"
    const val OFFICIAL_API = "https://api.kick.com/public/v1"
    const val TOKEN_URL = "https://id.kick.com/oauth/token"
    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Mobile Safari/537.36"

    fun isKickSource(source: String?, streamId: String? = null): Boolean {
        return source.equals(C.KICK, ignoreCase = true) || streamId.orEmpty().startsWith(STREAM_ID_PREFIX)
    }

    fun kickStreamId(id: String?): String {
        val raw = id?.removePrefix(STREAM_ID_PREFIX)?.takeIf { it.isNotBlank() } ?: "unknown"
        return "$STREAM_ID_PREFIX$raw"
    }

    fun webHeaders(cookieHeader: String? = null, accept: String = "application/json, text/plain, */*"): Map<String, String> {
        return buildMap {
            put("Accept", accept)
            put("Accept-Language", "en-US,en;q=0.9")
            put("Origin", WEB_ORIGIN)
            put("Referer", "$WEB_ORIGIN/")
            put("User-Agent", USER_AGENT)
            put("sec-fetch-dest", "empty")
            put("sec-fetch-mode", "cors")
            put("sec-fetch-site", "same-origin")
            put("x-app-platform", "web")
            put("x-kick-platform", "web")
            cookieHeader?.takeIf { it.isNotBlank() }?.let { put("Cookie", it) }
            xsrfToken(cookieHeader)?.let { put("X-XSRF-TOKEN", it) }
        }
    }

    fun htmlHeaders(cookieHeader: String? = null): Map<String, String> {
        return webHeaders(cookieHeader, accept = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    }

    fun officialHeaders(accessToken: String, clientId: String? = null): Map<String, String> {
        return buildMap {
            put("Accept", "application/json")
            put("Authorization", "Bearer $accessToken")
            put("User-Agent", USER_AGENT)
            clientId?.takeIf { it.isNotBlank() }?.let { put("Client-Id", it) }
        }
    }

    fun parseSetCookie(setCookie: String): Pair<String, String>? {
        val pair = setCookie.substringBefore(';').trim()
        val name = pair.substringBefore('=').trim()
        val value = pair.substringAfter('=', "").trim()
        if (name.isBlank() || value.isBlank() || name.lowercase() in RESERVED_COOKIE_ATTRS) {
            return null
        }
        return name to value
    }

    fun cookieHeader(cookies: Map<String, String>): String? {
        return cookies.entries
            .filter { it.key.isNotBlank() && it.value.isNotBlank() }
            .joinToString("; ") { "${it.key}=${it.value}" }
            .takeIf { it.isNotBlank() }
    }

    fun mergeCookieHeader(vararg headers: String?): String? {
        val cookies = linkedMapOf<String, String>()
        headers.forEach { header ->
            header?.split(';')?.forEach { part ->
                parseSetCookie(part)?.let { (name, value) -> cookies[name] = value }
            }
        }
        return cookieHeader(cookies)
    }

    fun cookieValue(cookieHeader: String?, name: String): String? {
        return cookieHeader
            ?.split(';')
            ?.asSequence()
            ?.map { it.trim() }
            ?.firstOrNull { it.startsWith("$name=", ignoreCase = true) }
            ?.substringAfter('=')
            ?.takeIf { it.isNotBlank() }
    }

    fun xsrfToken(cookieHeader: String?): String? {
        val raw = cookieValue(cookieHeader, "XSRF-TOKEN") ?: return null
        return runCatching { URLDecoder.decode(raw, Charsets.UTF_8.name()) }.getOrDefault(raw)
    }

    fun setCookieValues(headers: Map<String, List<String>>?): List<String> {
        return headers?.entries
            ?.firstOrNull { it.key.equals("Set-Cookie", ignoreCase = true) }
            ?.value
            .orEmpty()
    }

    fun isBlockedBody(body: String?): Boolean {
        return body.orEmpty().contains("Request blocked by security policy", ignoreCase = true)
    }

    fun labeledName(name: String?): String? {
        return name?.takeIf { it.isNotBlank() }?.let { "$it · Kick" }
    }

    fun isKickCdnUrl(url: String?): Boolean {
        return url.orEmpty().contains("kick.com", ignoreCase = true)
    }

    fun normalizePlaybackUrl(url: String?): String? {
        val normalizedUrl = url?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return when {
            normalizedUrl.endsWith(".m3u8", ignoreCase = true) -> normalizedUrl
            normalizedUrl.contains("kick.com", ignoreCase = true) &&
                (normalizedUrl.contains("/hls/", ignoreCase = true) ||
                    normalizedUrl.contains("/stream/", ignoreCase = true)) -> {
                if (normalizedUrl.endsWith("/")) {
                    "${normalizedUrl}playlist.m3u8"
                } else {
                    "$normalizedUrl/playlist.m3u8"
                }
            }
            else -> normalizedUrl
        }
    }

    fun channelShareUrl(channelLogin: String): String {
        return "https://kick.com/${channelLogin.trim().trimStart('/')}"
    }

    fun isReservedKickPath(slug: String): Boolean {
        return slug.lowercase() in RESERVED_PATHS
    }

    private val RESERVED_PATHS = setOf(
        "video", "videos", "clip", "clips", "category", "categories", "following",
        "directory", "team", "tags", "search", "settings", "dashboard", "login",
        "register", "browse", "home", "api", "emotes", "community-guidelines",
    )

    private val RESERVED_COOKIE_ATTRS = setOf(
        "path", "domain", "expires", "max-age", "secure", "httponly", "samesite",
    )
}
