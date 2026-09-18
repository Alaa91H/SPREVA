package com.spreva.feature.lesson.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spreva.core.audio.TtsStatus
import com.spreva.core.designsystem.theme.sprevaArticleColor
import com.spreva.core.model.LearningActivity
import com.spreva.core.model.Lesson

/**
 * Lesson player: one primary task per screen (plan section 49). The
 * renderer maps each activity type to a dedicated composable (plan
 * sections 72-74), so new activity types never touch the shell.
 */
@Composable
fun LessonRoute(
    lessonId: String,
    onFinished: () -> Unit,
    viewModel: LessonViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(lessonId) {
        viewModel.load(lessonId)
    }

    LessonScreen(
        state = state,
        onAnswerChanged = viewModel::onAnswerChanged,
        onOptionSelected = viewModel::onOptionSelected,
        onCheck = viewModel::check,
        onNext = viewModel::next,
        onRetry = { viewModel.load(lessonId) },
        onSpeakCurrent = viewModel::speakCurrent,
        onSpeakWord = viewModel::speakWord,
        onFinished = {
            viewModel.finish()
            onFinished()
        },
    )
}

@Composable
internal fun LessonScreen(
    state: LessonUiState,
    onAnswerChanged: (String) -> Unit,
    onOptionSelected: (String) -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit,
    onRetry: () -> Unit,
    onSpeakCurrent: () -> Unit,
    onSpeakWord: (String?, String) -> Unit,
    onFinished: () -> Unit,
) {
    when {
        state.isLoading -> Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            Text(stringResource(R.string.spreva_lesson_loading))
        }

        state.error != null -> Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.spreva_lesson_content_error))
            Button(onClick = onRetry) { Text(stringResource(R.string.spreva_lesson_retry)) }
        }

        state.lesson != null -> LessonContent(
            state = state,
            onAnswerChanged = onAnswerChanged,
            onOptionSelected = onOptionSelected,
            onCheck = onCheck,
            onNext = onNext,
            onSpeakCurrent = onSpeakCurrent,
            onSpeakWord = onSpeakWord,
            onFinished = onFinished,
        )
    }
}

@Composable
private fun LessonContent(
    state: LessonUiState,
    onAnswerChanged: (String) -> Unit,
    onOptionSelected: (String) -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit,
    onSpeakCurrent: () -> Unit,
    onSpeakWord: (String?, String) -> Unit,
    onFinished: () -> Unit,
) {
    val lesson = state.lesson ?: return
    val activity = lesson.activities.getOrNull(state.currentIndex)

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // Lightweight header: "3 / 8" + thin progress (plan section 50).
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(
                text = "${state.currentIndex + 1} / ${lesson.activities.size}",
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.weight(1f))
            // Phase 4.1: hear the German prompt (plan sections 31-34).
            if (state.ttsStatus == TtsStatus.READY) {
                IconButton(onClick = onSpeakCurrent) {
                    Icon(
                        imageVector = Icons.Filled.VolumeUp,
                        contentDescription = stringResource(R.string.spreva_lesson_speak),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = {
                (state.currentIndex + 1).toFloat() / lesson.activities.size.toFloat()
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))

        activity?.let { current ->
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (current) {
                    is LearningActivity.TextIntro -> TextIntroRenderer(current)
                    is LearningActivity.VocabularyIntro -> VocabularyRenderer(
                        activity = current,
                        onSpeakWord = onSpeakWord,
                    )
                    is LearningActivity.MultipleChoice -> MultipleChoiceRenderer(
                        activity = current,
                        selectedOptionId = state.selectedOptionId,
                        answerState = state.answerState,
                        onOptionSelected = onOptionSelected,
                    )

                    is LearningActivity.Cloze -> ClozeRenderer(
                        activity = current,
                        typedAnswer = state.typedAnswer,
                        answerState = state.answerState,
                        onAnswerChanged = onAnswerChanged,
                    )

                    is LearningActivity.LessonSummaryActivity -> SummaryRenderer(current, state.completedCount)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Feedback banner between check and next.
        state.answerState?.let { answerState ->
            val correctText = stringResource(R.string.spreva_lesson_correct)
            val incorrectText = stringResource(R.string.spreva_lesson_incorrect)
            Text(
                text = if (answerState.correct) correctText else incorrectText,
                style = MaterialTheme.typography.titleMedium,
                color = if (answerState.correct) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            if (!answerState.correct) {
                val solution = answerState.solution
                if (solution != null) {
                    Text(
                        text = stringResource(R.string.spreva_lesson_correct_answer, solution),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        val isSummary = activity is LearningActivity.LessonSummaryActivity
        val checkLabel = stringResource(R.string.spreva_lesson_check)
        val nextLabel = stringResource(R.string.spreva_lesson_next)
        val finishLabel = stringResource(R.string.spreva_lesson_finish)

        Button(
            onClick = if (isSummary) onFinished else if (state.answerState == null) onCheck else onNext,
            enabled = if (isSummary) {
                true
            } else {
                state.answerState != null || state.canCheck
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                when {
                    isSummary -> finishLabel
                    state.answerState != null -> nextLabel
                    else -> checkLabel
                },
            )
        }
    }
}

/** Big, selectable German text (plan sections 37/51). */
@Composable
private fun TextIntroRenderer(activity: LearningActivity.TextIntro) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(activity.title.de, style = MaterialTheme.typography.headlineSmall)
        Text(activity.body.de, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun VocabularyRenderer(
    activity: LearningActivity.VocabularyIntro,
    onSpeakWord: (String?, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        activity.words.forEach { word ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            word.article?.let { article ->
                                Text(
                                    text = article,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = sprevaArticleColor(article),
                                )
                            }
                            Text(
                                text = word.german,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                            )
                        }
                        word.plural?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        Text(
                            text = word.translation.ar ?: word.translation.de,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    // Phase 4.1: hear the word (article + noun) via built-in TTS.
                    IconButton(onClick = { onSpeakWord(word.article, word.german) }) {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = stringResource(R.string.spreva_lesson_speak_word, word.german),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MultipleChoiceRenderer(
    activity: LearningActivity.MultipleChoice,
    selectedOptionId: String?,
    answerState: AnswerState?,
    onOptionSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(activity.question.de, style = MaterialTheme.typography.titleLarge)
        activity.options.forEach { option ->
            val selected = selectedOptionId == option.id
            Card(
                onClick = { if (answerState == null) onOptionSelected(option.id) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selected, onClick = null)
                    Text(option.text.de, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun ClozeRenderer(
    activity: LearningActivity.Cloze,
    typedAnswer: String,
    answerState: AnswerState?,
    onAnswerChanged: (String) -> Unit,
) {
    val parts = activity.sentenceTemplate.de.split(activity.answerPlaceholder)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = parts.getOrElse(0) { "" },
            style = MaterialTheme.typography.titleLarge,
        )
        OutlinedTextField(
            value = typedAnswer,
            onValueChange = onAnswerChanged,
            enabled = answerState == null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.spreva_lesson_type_answer)) },
        )
        if (parts.size > 1) {
            Text(
                text = parts.drop(1).joinToString(" "),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun SummaryRenderer(activity: LearningActivity.LessonSummaryActivity, completedCount: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(activity.title.de, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(R.string.spreva_lesson_completed),
            style = MaterialTheme.typography.bodyLarge,
        )
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Can do:", style = MaterialTheme.typography.titleMedium)
                activity.canDo.forEach { canDo ->
                    Text("✓ ${canDo.value}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
