package com.spreva.core.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire DTOs for the bundled JSON content schema (schemaVersion 1).
 * Parsed and validated here, mapped to core:model types afterwards.
 */
@Serializable
data class ContentManifestDto(
    @SerialName("schemaVersion") val schemaVersion: Int,
    @SerialName("contentVersion") val contentVersion: String,
    @SerialName("packages") val packages: List<ContentPackageRefDto> = emptyList(),
)

@Serializable
data class ContentPackageRefDto(
    @SerialName("id") val id: String,
    @SerialName("courseId") val courseId: String,
    @SerialName("level") val level: String,
)

@Serializable
data class CourseDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: LocalizedTextDto,
    @SerialName("levels") val levels: List<LevelDto> = emptyList(),
)

@Serializable
data class LevelDto(
    @SerialName("id") val id: String,
    @SerialName("cefr") val cefr: String,
    @SerialName("title") val title: LocalizedTextDto,
    @SerialName("units") val units: List<UnitDto> = emptyList(),
)

@Serializable
data class UnitDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: LocalizedTextDto,
    @SerialName("lessons") val lessons: List<LessonRefDto> = emptyList(),
)

@Serializable
data class LessonRefDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: LocalizedTextDto? = null,
    @SerialName("activityCount") val activityCount: Int = 0,
)

@Serializable
data class LessonDto(
    @SerialName("id") val id: String,
    @SerialName("unitId") val unitId: String,
    @SerialName("title") val title: LocalizedTextDto,
    @SerialName("canDo") val canDo: List<String> = emptyList(),
    @SerialName("activities") val activities: List<ActivityDto> = emptyList(),
)

@Serializable
data class LocalizedTextDto(
    @SerialName("de") val de: String,
    @SerialName("ar") val ar: String? = null,
    @SerialName("en") val en: String? = null,
)

@Serializable
data class ActivityDto(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @SerialName("title") val title: LocalizedTextDto? = null,
    @SerialName("body") val body: LocalizedTextDto? = null,
    @SerialName("words") val words: List<VocabularyItemDto> = emptyList(),
    @SerialName("prompt") val prompt: LocalizedTextDto? = null,
    @SerialName("question") val question: LocalizedTextDto? = null,
    /** listening_choice: bundled recording path, relative to content/. */
    @SerialName("audio") val audio: String? = null,
    @SerialName("options") val options: List<ChoiceOptionDto> = emptyList(),
    @SerialName("correctOptionId") val correctOptionId: String? = null,
    /** speaking_repeat/dictation/listening TTS fallback: hidden German target text. */
    @SerialName("text") val text: LocalizedTextDto? = null,
    /** free_write: minimum required word count. */
    @SerialName("minWords") val minWords: Int? = null,
    /** speaking_prompt: minimum recording duration. */
    @SerialName("minSeconds") val minSeconds: Int? = null,
    /** free_write/speaking_prompt: learner self-review points. */
    @SerialName("checklist") val checklist: List<LocalizedTextDto> = emptyList(),
    @SerialName("sentenceTemplate") val sentenceTemplate: LocalizedTextDto? = null,
    @SerialName("answerPlaceholder") val answerPlaceholder: String? = null,
    @SerialName("acceptedAnswers") val acceptedAnswers: List<String> = emptyList(),
    @SerialName("canDo") val canDo: List<String> = emptyList(),
)

@Serializable
data class VocabularyItemDto(
    @SerialName("id") val id: String,
    @SerialName("german") val german: String,
    @SerialName("article") val article: String? = null,
    @SerialName("plural") val plural: String? = null,
    @SerialName("translation") val translation: LocalizedTextDto,
    /** Bundled recording path relative to content/, e.g. "audio/cc-by-sa/de-hallo.ogg". */
    @SerialName("audio") val audio: String? = null,
)

@Serializable
data class ChoiceOptionDto(
    @SerialName("id") val id: String,
    @SerialName("text") val text: LocalizedTextDto,
)

/** Wire DTO for the media provenance registry (content/audio/licenses.json). */
@Serializable
data class MediaLicensesDto(
    @SerialName("registryVersion") val registryVersion: Int = 1,
    @SerialName("downloadDate") val downloadDate: String? = null,
    @SerialName("reviewedBy") val reviewedBy: String? = null,
    @SerialName("sources") val sources: List<MediaSourceDto> = emptyList(),
    @SerialName("files") val files: List<MediaFileLicenseDto> = emptyList(),
    @SerialName("notes") val notes: List<String> = emptyList(),
)

@Serializable
data class MediaSourceDto(
    @SerialName("name") val name: String,
    @SerialName("url") val url: String,
    @SerialName("license") val license: String? = null,
    @SerialName("attribution") val attribution: String? = null,
)

@Serializable
data class MediaFileLicenseDto(
    @SerialName("path") val path: String,
    @SerialName("sourceUrl") val sourceUrl: String,
    @SerialName("mediaUrl") val mediaUrl: String? = null,
    @SerialName("license") val license: String,
    @SerialName("licenseUrl") val licenseUrl: String? = null,
    @SerialName("attribution") val attribution: String,
    @SerialName("sha256") val sha256: String? = null,
)
