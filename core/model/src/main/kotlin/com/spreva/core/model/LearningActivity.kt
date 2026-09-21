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

    /**
     * Listening comprehension (Phase 4.2): the learner hears a bundled
     * recording and picks the matching phrase. The correct answer is
     * never spoken by TTS in this activity.
     */
    data class ListeningChoice(
        override val id: ActivityId,
        /** Bundled recording path, relative to content/. Null enables local TTS fallback. */
        val audio: String? = null,
        /** Hidden German source text used only when native audio is unavailable. */
        val text: LocalizedText? = null,
        /** Optional visible task instruction. */
        val prompt: LocalizedText? = null,
        val options: List<ChoiceOption>,
        val correctOptionId: String,
    ) : LearningActivity

    /**
     * Shadowing starter (Phase 4.3): the learner plays a bundled native
     * recording, records themselves repeating it, and sees a simplified
     * envelope comparison between both waveforms. Self-check — no server,
     * no ASR scoring yet (plan sections 35/38).
     */
    data class SpeakingRepeat(
        override val id: ActivityId,
        /** Bundled model recording, relative to content/. Null uses local German TTS. */
        val audio: String? = null,
        val prompt: LocalizedText? = null,
        /** The phrase the learner should repeat. */
        val text: LocalizedText,
    ) : LearningActivity

    /** Listen without visible target text and type exactly what was heard. */
    data class Dictation(
        override val id: ActivityId,
        val audio: String? = null,
        val prompt: LocalizedText? = null,
        /** Hidden target; also serves as offline TTS fallback. */
        val text: LocalizedText,
        val acceptedAnswers: List<String>,
    ) : LearningActivity

    /** Free written production. Completion is based on task length, not linguistic correctness. */
    data class FreeWrite(
        override val id: ActivityId,
        val prompt: LocalizedText,
        val minWords: Int,
        val checklist: List<LocalizedText> = emptyList(),
    ) : LearningActivity

    /** Free spoken production recorded locally; no model answer or fake pronunciation grade. */
    data class SpeakingPrompt(
        override val id: ActivityId,
        val prompt: LocalizedText,
        val minSeconds: Int,
        val checklist: List<LocalizedText> = emptyList(),
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
