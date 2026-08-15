package com.github.andreyasadchy.xtra.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KickWebsiteSearchRequestTest {

    @Test
    fun buildUrl_encodesQuery() {
        assertEquals(
            "https://kick.com/api/search?searched_word=xqc",
            KickWebsiteSearchRequest.buildUrl("xqc"),
        )
        assertEquals(
            "https://kick.com/api/search?searched_word=just+chatting",
            KickWebsiteSearchRequest.buildUrl(" just chatting "),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun buildUrl_requiresMinimumLength() {
        KickWebsiteSearchRequest.buildUrl("xq")
    }

    @Test
    fun minQueryLength_isThree() {
        assertTrue(KickWebsiteSearchRequest.MIN_QUERY_LENGTH == 3)
    }
}
