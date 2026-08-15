package com.github.andreyasadchy.xtra.repository

internal object KickWebsiteSearchRequest {
    const val MIN_QUERY_LENGTH = 3
    private const val BASE_URL = "https://kick.com/api/search"
    private const val QUERY_PARAM = "searched_word"

    fun buildUrl(query: String): String {
        val normalizedQuery = query.trim()
        require(normalizedQuery.length >= MIN_QUERY_LENGTH) {
            "Kick website search requires at least $MIN_QUERY_LENGTH characters"
        }
        return "$BASE_URL?$QUERY_PARAM=${java.net.URLEncoder.encode(normalizedQuery, Charsets.UTF_8.name())}"
    }
}
