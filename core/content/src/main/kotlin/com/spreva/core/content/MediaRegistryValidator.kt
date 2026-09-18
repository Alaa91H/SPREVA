package com.spreva.core.content

import java.io.File
import java.security.MessageDigest

/**
 * Release-grade integrity validation of the media provenance registry
 * (audit §12). The attribution UI is only as trustworthy as the data
 * behind it, so CI enforces:
 *
 * 1. every registry entry points at an existing distributed file
 * 2. every distributed media file has a registry entry
 * 3. required metadata present (license, licenseUrl, attribution, sourceUrl)
 * 4. actual SHA-256 matches the registered digest
 * 5. no duplicate entries
 * 6. license policy: only permissively share-alike CC BY-SA family files
 */
object MediaRegistryValidator {

    /** Only licenses the project's media policy currently accepts. */
    private val ALLOWED_LICENSES = setOf(
        "CC BY-SA 4.0",
        "CC BY-SA 3.0",
    )

    /**
     * @param registry parsed licenses.json
     * @param mediaRoot directory containing the distributed audio files;
     *   registry paths are relative to the content package root, so a
     *   resolver maps `content/audio/...` to a concrete file.
     * @return list of violations; empty list = valid registry.
     */
    fun validate(
        registry: MediaLicensesDto,
        mediaRoot: File?,
        distributedFiles: Set<String> = emptySet(),
    ): List<String> = buildList {
        val seen = mutableSetOf<String>()

        registry.files.forEach { entry ->
            if (entry.path.isBlank()) {
                add("Registry entry with blank path")
                return@forEach
            }
            if (!seen.add(entry.path)) {
                add("Duplicate registry entry for ${entry.path}")
            }
            if (entry.license.isBlank()) add("${entry.path}: missing license")
            if (entry.licenseUrl.isNullOrBlank()) add("${entry.path}: missing licenseUrl")
            if (entry.attribution.isBlank()) add("${entry.path}: missing attribution")
            if (entry.sourceUrl.isNullOrBlank()) add("${entry.path}: missing sourceUrl")
            if (entry.license !in ALLOWED_LICENSES) {
                add("${entry.path}: license '${entry.license}' violates project media policy")
            }
            if (entry.sha256.isNullOrBlank()) {
                add("${entry.path}: missing sha256 digest")
            } else if (mediaRoot != null) {
                val file = mediaRoot.resolve(entry.path.removePrefix("content/"))
                when {
                    !file.exists() -> add("${entry.path}: file missing from distribution")
                    else -> {
                        val actual = file.sha256()
                        if (!actual.equals(entry.sha256, ignoreCase = true)) {
                            add("${entry.path}: sha256 mismatch (registry=${entry.sha256.take(12)}… actual=${actual.take(12)}…)")
                        }
                    }
                }
            }
        }

        // Every distributed media file must be covered by the registry.
        if (mediaRoot != null) {
            val onDisk = mediaRoot.walkTopDown()
                .filter { it.isFile && it.extension.lowercase() in setOf("ogg", "oga", "mp3", "m4a", "aac", "wav", "flac") }
                .map { "content/audio/" + it.relativeTo(mediaRoot).invariantSeparatorsPath }
                .toSet()
            val registered = registry.files.map { it.path }.toSet()
            (onDisk - registered).forEach { add("$it: distributed media without registry entry") }
        }

        // Bundled-lesson references must exist in the registry.
        (distributedFiles - registry.files.map { it.path }.toSet()).forEach {
            add("$it: referenced by content but absent from media registry")
        }
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
