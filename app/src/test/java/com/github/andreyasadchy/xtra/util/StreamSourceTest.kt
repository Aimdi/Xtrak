package com.github.andreyasadchy.xtra.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamSourceTest {

    @Test
    fun includeTwitch_isDefault() {
        assertTrue(StreamSource.includeTwitch(null))
        assertTrue(StreamSource.includeTwitch(C.TWITCH))
        assertTrue(StreamSource.includeTwitch("TWITCH"))
        assertFalse(StreamSource.includeTwitch(C.KICK))
    }

    @Test
    fun includeKick_onlyWhenSelected() {
        assertFalse(StreamSource.includeKick(null))
        assertFalse(StreamSource.includeKick(C.TWITCH))
        assertTrue(StreamSource.includeKick(C.KICK))
        assertTrue(StreamSource.includeKick("KICK"))
    }
}
