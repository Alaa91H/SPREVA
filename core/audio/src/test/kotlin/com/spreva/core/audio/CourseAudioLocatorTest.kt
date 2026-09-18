package com.spreva.core.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CourseAudioLocatorTest {

    /** Fake opener mirroring the app's asset opener contract. */
    private val fakeOpener = { path: String ->
        if (path.startsWith("audio/")) "asset:///content/$path" else null
    }

    @Test
    fun `resolves content path to uri string`() {
        val locator = CourseAudioLocator(fakeOpener)
        assertEquals(
            "asset:///content/audio/cc-by-sa/de-hallo.ogg",
            locator.resolve("audio/cc-by-sa/de-hallo.ogg"),
        )
    }

    @Test
    fun `unknown file resolves to null so TTS fallback kicks in`() {
        val locator = CourseAudioLocator { _ -> null }
        assertNull(locator.resolve("audio/cc-by-sa/does-not-exist.ogg"))
    }

    @Test
    fun `blank and null paths resolve to null`() {
        val locator = CourseAudioLocator(fakeOpener)
        assertNull(locator.resolve(null))
        assertNull(locator.resolve("   "))
    }

    @Test
    fun `blank opener result is treated as missing`() {
        val locator = CourseAudioLocator { _ -> "  " }
        assertNull(locator.resolve("audio/x.ogg"))
    }
}
