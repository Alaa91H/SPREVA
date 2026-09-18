package com.spreva.core.content

import com.spreva.core.model.Lesson

/**
 * Structural validation of parsed content (plan section 68):
 * non-empty IDs, no duplicates, known answers, non-empty options,
 * valid activity references.
 */
object ContentValidator {

    fun validateLesson(lesson: Lesson): List<String> = buildList {
        if (lesson.id.value.isBlank()) add("Lesson id is blank")
        if (lesson.activities.isEmpty()) add("Lesson ${lesson.id.value} has no activities")

        val activityIds = lesson.activities.map { it.id.value }
        activityIds.filter { it.isBlank() }.forEach { add("Blank activity id in lesson ${lesson.id.value}") }
        val duplicates = activityIds.groupBy { it }.filter { it.value.size > 1 }.keys
        if (duplicates.isNotEmpty()) add("Duplicate activity ids in ${lesson.id.value}: $duplicates")

        lesson.activities.forEach { activity ->
            when (activity) {
                is com.spreva.core.model.LearningActivity.MultipleChoice -> {
                    if (activity.options.isEmpty()) add("MCQ ${activity.id.value} has no options")
                    if (activity.options.size < 2) add("MCQ ${activity.id.value} needs at least 2 options")
                    if (activity.options.none { it.id == activity.correctOptionId }) {
                        add("MCQ ${activity.id.value} correctOptionId '${activity.correctOptionId}' not in options")
                    }
                }

                is com.spreva.core.model.LearningActivity.Cloze -> {
                    if (activity.acceptedAnswers.isEmpty()) add("Cloze ${activity.id.value} has no accepted answers")
                    if (activity.acceptedAnswers.any { it.isBlank() }) {
                        add("Cloze ${activity.id.value} has blank accepted answers")
                    }
                    if (!activity.sentenceTemplate.de.contains(activity.answerPlaceholder)) {
                        add("Cloze ${activity.id.value} template missing placeholder '${activity.answerPlaceholder}'")
                    }
                }

                is com.spreva.core.model.LearningActivity.VocabularyIntro -> {
                    if (activity.words.isEmpty()) add("Vocab ${activity.id.value} has no words")
                    val wordIds = activity.words.map { it.id }
                    if (wordIds.size != wordIds.toSet().size) {
                        add("Vocab ${activity.id.value} has duplicate word ids")
                    }
                    activity.words.filter { it.german.isBlank() }.forEach {
                        add("Vocab ${activity.id.value} word ${it.id} has blank german")
                    }
                }

                else -> Unit
            }
        }
    }
}
