package com.github.andreyasadchy.xtra.util

object KickApiHelper {
    const val STREAM_ID_PREFIX = "kick:"
    const val WEB_ORIGIN = "https://kick.com"
    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

    fun isKickSource(source: String?, streamId: String? = null): Boolean {
        return source.equals(C.KICK, ignoreCase = true) || streamId.orEmpty().startsWith(STREAM_ID_PREFIX)
    }

    fun kickStreamId(id: String?): String {
        val raw = id?.removePrefix(STREAM_ID_PREFIX)?.takeIf { it.isNotBlank() } ?: "unknown"
        return "$STREAM_ID_PREFIX$raw"
    }

    fun webHeaders(): Map<String, String> {
        return mapOf(
            "Accept" to "application/json, text/plain, */*",
            "Origin" to WEB_ORIGIN,
            "Referer" to "$WEB_ORIGIN/",
            "User-Agent" to USER_AGENT,
            "x-kick-platform" to "web",
        )
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
}
