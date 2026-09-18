package com.spreva.core.content

import com.spreva.core.model.ActivityId
import com.spreva.core.model.CanDoId
import com.spreva.core.model.CefrLevel
import com.spreva.core.model.ChoiceOption
import com.spreva.core.model.Course
import com.spreva.core.model.CourseId
import com.spreva.core.model.LearningActivity
import com.spreva.core.model.Lesson
import com.spreva.core.model.LessonId
import com.spreva.core.model.LessonSummary
import com.spreva.core.model.Level
import com.spreva.core.model.LevelId
import com.spreva.core.model.LocalizedText
import com.spreva.core.model.Unit
import com.spreva.core.model.UnitId
import com.spreva.core.model.VocabularyItem
import kotlinx.serialization.json.Json

/**
 * Parses bundled JSON content into validated DTOs, then maps to
 * core:model types. Rejects unsupported schema versions with a clear error
 * (plan section 198).
 */
class ContentParser(
    json: Json = defaultJson,
) {
    private val json = json

    fun parseManifest(text: String): ContentManifestDto {
        val manifest = json.decodeFromString<ContentManifestDto>(text)
        if (manifest.schemaVersion > SUPPORTED_SCHEMA_VERSION) {
            throw ContentValidationException(
                "Content schema version ${manifest.schemaVersion} is newer than supported " +
                    "version $SUPPORTED_SCHEMA_VERSION. Update the app.",
            )
        }
        return manifest
    }

    fun parseCourse(text: String): CourseDto = json.decodeFromString<CourseDto>(text)

    fun parseLesson(text: String): LessonDto = json.decodeFromString<LessonDto>(text)

    fun toCourse(dto: CourseDto): Course = Course(
        id = CourseId(dto.id),
        title = dto.title.toModel(),
        levels = dto.levels.map { levelDto ->
            Level(
                id = LevelId(levelDto.id),
                cefr = parseCefr(levelDto.cefr),
                title = levelDto.title.toModel(),
                units = levelDto.units.map { unitDto ->
                    Unit(
                        id = UnitId(unitDto.id),
                        title = unitDto.title.toModel(),
                        lessons = unitDto.lessons.map { LessonSummary(LessonId(it.id), LocalizedText(""), 0) },
                    )
                },
            )
        },
    )

    fun toLesson(dto: LessonDto): Lesson = Lesson(
        id = LessonId(dto.id),
        unitId = UnitId(dto.unitId),
        title = dto.title.toModel(),
        canDo = dto.canDo.map(::CanDoId),
        activities = dto.activities.map { it.toModel() },
    )

    companion object {
        const val SUPPORTED_SCHEMA_VERSION = 1

        val defaultJson: Json = Json {
            ignoreUnknownKeys = true
            isLenient = false
            encodeDefaults = false
        }

        fun parseCefr(value: String): CefrLevel = when (value.uppercase()) {
            "PRE-A1", "PRE_A1" -> CefrLevel.PRE_A1
            "A1" -> CefrLevel.A1
            "A2" -> CefrLevel.A2
            "B1" -> CefrLevel.B1
            "B2" -> CefrLevel.B2
            "C1" -> CefrLevel.C1
            else -> throw ContentValidationException("Unknown CEFR level: $value")
        }
    }
}

private fun LocalizedTextDto.toModel() = LocalizedText(de = de, ar = ar, en = en)

private fun ActivityDto.toModel(): LearningActivity = when (type) {
    "text_intro" -> LearningActivity.TextIntro(
        id = ActivityId(id),
        title = title?.toModel() ?: throw ContentValidationException("text_intro $id missing title"),
        body = body?.toModel() ?: throw ContentValidationException("text_intro $id missing body"),
    )

    "vocab_intro" -> LearningActivity.VocabularyIntro(
        id = ActivityId(id),
        words = words.map {
            VocabularyItem(
                id = it.id,
                german = it.german,
                article = it.article,
                plural = it.plural,
                translation = it.translation.toModel(),
                audio = it.audio,
            )
        },
    )

    "multiple_choice" -> LearningActivity.MultipleChoice(
        id = ActivityId(id),
        prompt = prompt?.toModel() ?: throw ContentValidationException("multiple_choice $id missing prompt"),
        question = question?.toModel() ?: throw ContentValidationException("multiple_choice $id missing question"),
        options = options.map { ChoiceOption(it.id, it.text.toModel()) },
        correctOptionId = correctOptionId
            ?: throw ContentValidationException("multiple_choice $id missing correctOptionId"),
    )

    "listening_choice" -> LearningActivity.ListeningChoice(
        id = ActivityId(id),
        audio = audio
            ?: throw ContentValidationException("listening_choice $id missing audio"),
        prompt = prompt?.toModel(),
        options = options.map { ChoiceOption(it.id, it.text.toModel()) },
        correctOptionId = correctOptionId
            ?: throw ContentValidationException("listening_choice $id missing correctOptionId"),
    )

    "cloze" -> LearningActivity.Cloze(
        id = ActivityId(id),
        sentenceTemplate = sentenceTemplate?.toModel()
            ?: throw ContentValidationException("cloze $id missing sentenceTemplate"),
        answerPlaceholder = answerPlaceholder ?: "{answer}",
        acceptedAnswers = acceptedAnswers,
    )

    "lesson_summary" -> LearningActivity.LessonSummaryActivity(
        id = ActivityId(id),
        title = title?.toModel() ?: throw ContentValidationException("lesson_summary $id missing title"),
        canDo = canDo.map(::CanDoId),
    )

    else -> throw ContentValidationException("Unknown activity type '$type' in activity $id")
}

/** Thrown when content fails validation. */
class ContentValidationException(message: String) : Exception(message)
