package com.spreva.core.model

/**
 * Learning activity types rendered by the lesson renderer (Phase 3 set).
 *
 * Adding a new activity type means adding a model here, a renderer in the
 * lesson feature, and a content schema entry — existing lessons untouched.
 */
sealed interface LearningActivity {
    val id: ActivityId

    data class TextIntro(
        override val id: ActivityId,
        val title: LocalizedText,
        val body: LocalizedText,
    ) : LearningActivity

    data class VocabularyIntro(
        override val id: ActivityId,
        val words: List<VocabularyItem>,
    ) : LearningActivity

    data class MultipleChoice(
        override val id: ActivityId,
        val prompt: LocalizedText,
        val question: LocalizedText,
        val options: List<ChoiceOption>,
        val correctOptionId: String,
    ) : LearningActivity

    data class Cloze(
        override val id: ActivityId,
        val sentenceTemplate: LocalizedText,
        /** Placeholder in the template that must be filled, e.g. "{answer}". */
        val answerPlaceholder: String = "{answer}",
        val acceptedAnswers: List<String>,
    ) : LearningActivity

    data class LessonSummaryActivity(
        override val id: ActivityId,
        val title: LocalizedText,
        val canDo: List<CanDoId>,
    ) : LearningActivity
}

data class VocabularyItem(
    val id: String,
    val german: String,
    val article: String? = null,
    val plural: String? = null,
    val translation: LocalizedText,
    /** Path of a bundled native-speaker recording, relative to content/, or null. */
    val audio: String? = null,
)

data class ChoiceOption(
    val id: String,
    val text: LocalizedText,
)
