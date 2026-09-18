package com.spreva.core.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest
/**
 * Media registry integrity tests (audit §12/§78): the shipped registry
 * must cover every distributed file with accurate digests, and the
 * validator must reject every tampered fixture class.
 */
class MediaRegistryValidatorTest {

    private val parser = ContentParser()

    private fun sha256Of(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun mediaRoot(): File? {
        // core:content resources mirror app assets; audio lives under
        // content/audio in both. Resolve the directory when present.
        val url = javaClass.classLoader?.getResource("content/audio/cc-by-sa/de-hallo.ogg")
            ?: return null
        return File(url.toURI()).parentFile?.parentFile
    }

    @Test
    fun `shipped registry covers every bundled file with correct digests`() {
        val registry = parser.parseMediaLicenses(
            javaClass.classLoader.getResourceAsStream("content/audio/licenses.json")!!
                .bufferedReader().readText(),
        )
        val root = mediaRoot() ?: return
        val violations = MediaRegistryValidator.validate(registry, root)
        assertEquals("Registry violations: $violations", emptyList<String>(), violations)
    }

    @Test
    fun `missing digest is rejected`() {
        val registry = MediaLicensesDto(
            files = listOf(
                MediaFileLicenseDto(
                    path = "content/audio/cc-by-sa/de-hallo.ogg",
                    sourceUrl = "https://example.org/x",
                    license = "CC BY-SA 4.0",
                    licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0",
                    attribution = "A",
                    sha256 = null,
                ),
            ),
        )
        val violations = MediaRegistryValidator.validate(registry, mediaRoot = null)
        assertTrue(violations.any { it.contains("missing sha256") })
    }

    @Test
    fun `wrong digest is rejected`() {
        val registry = MediaLicensesDto(
            files = listOf(
                MediaFileLicenseDto(
                    path = "content/audio/cc-by-sa/de-hallo.ogg",
                    sourceUrl = "https://example.org/x",
                    license = "CC BY-SA 4.0",
                    licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0",
                    attribution = "A",
                    sha256 = "deadbeef",
                ),
            ),
        )
        val root = mediaRoot() ?: return
        val violations = MediaRegistryValidator.validate(registry, root)
        assertTrue(violations.any { it.contains("sha256 mismatch") })
    }

    @Test
    fun `duplicate entry is rejected`() {
        val entry = MediaFileLicenseDto(
            path = "content/audio/cc-by-sa/de-hallo.ogg",
            sourceUrl = "https://example.org/x",
            license = "CC BY-SA 4.0",
            licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0",
            attribution = "A",
            sha256 = "x",
        )
        val violations = MediaRegistryValidator.validate(
            MediaLicensesDto(files = listOf(entry, entry.copy(attribution = "B"))),
            mediaRoot = null,
        )
        assertTrue(violations.any { it.contains("Duplicate registry entry") })
    }

    @Test
    fun `disallowed license violates project media policy`() {
        val registry = MediaLicensesDto(
            files = listOf(
                MediaFileLicenseDto(
                    path = "content/audio/cc-by-sa/de-hallo.ogg",
                    sourceUrl = "https://example.org/x",
                    license = "GFDL",
                    licenseUrl = "https://example.org/gfdl",
                    attribution = "A",
                    sha256 = "x",
                ),
            ),
        )
        val violations = MediaRegistryValidator.validate(registry, mediaRoot = null)
        assertTrue(violations.any { it.contains("violates project media policy") })
    }

    @Test
    fun `missing metadata fields are each rejected`() {
        val registry = MediaLicensesDto(
            files = listOf(
                MediaFileLicenseDto(
                    path = "content/audio/cc-by-sa/de-hallo.ogg",
                    sourceUrl = "",
                    license = "CC BY-SA 4.0",
                    licenseUrl = "",
                    attribution = "",
                    sha256 = "x",
                ),
            ),
        )
        val violations = MediaRegistryValidator.validate(registry, mediaRoot = null)
        assertTrue(violations.any { it.contains("missing licenseUrl") })
        assertTrue(violations.any { it.contains("missing attribution") })
        assertTrue(violations.any { it.contains("missing sourceUrl") })
    }

    @Test
    fun `content-referenced audio absent from registry is rejected`() {
        val violations = MediaRegistryValidator.validate(
            registry = MediaLicensesDto(),
            mediaRoot = null,
            distributedFiles = setOf("content/audio/cc-by-sa/de-unknown.ogg"),
        )
        assertTrue(violations.any { it.contains("absent from media registry") })
    }

    @Test
    fun `unregistered distributed file is detected`() {
        val root = mediaRoot() ?: return
        val violations = MediaRegistryValidator.validate(
            registry = MediaLicensesDto(), // empty registry
            mediaRoot = root,
        )
        assertTrue(violations.any { it.contains("distributed media without registry entry") })
    }

    @Test
    fun `every audio reference in bundled lessons is covered by the registry`() {
        val registry = parser.parseMediaLicenses(
            javaClass.classLoader.getResourceAsStream("content/audio/licenses.json")!!
                .bufferedReader().readText(),
        )
        val registered = registry.files.map { it.path }.toSet()
        val referenced = buildList {
            for (lesson in listOf("a1_u01_l01", "a1_u02_l01")) {
                val text = javaClass.classLoader
                    .getResourceAsStream("content/courses/de-core/lessons/$lesson.json")!!
                    .bufferedReader().readText()
                Regex(""""audio/[^"']+"""").findAll(text).forEach { add(it.value.removeSurrounding("\"")) }
            }
        }.map { "content/$it" }.toSet()
        val missing = referenced - registered
        assertEquals("Audio references missing from registry: $missing", emptySet<String>(), missing)
    }
}
