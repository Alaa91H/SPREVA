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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spreva.core.audio.TtsStatus
import com.spreva.core.designsystem.theme.sprevaArticleColor
import com.spreva.core.model.LearningActivity
import com.spreva.core.model.Lesson
import com.spreva.core.model.RubricRating

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
    val context = androidx.compose.ui.platform.LocalContext.current

    // Phase 4.3 + audit §10: a granted permission starts recording in the
    // same interaction — the empty callback used to force a second tap.
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.startRecording()
    }

    LaunchedEffect(lessonId) {
        viewModel.load(lessonId)
    }

    // Audit §8: navigate only after completion persistence succeeded.
    LaunchedEffect(state.completionState) {
        if (state.completionState == LessonCompletionState.Success) onFinished()
    }

    LessonScreen(
        state = state,
        onAnswerChanged = viewModel::onAnswerChanged,
        onOptionSelected = viewModel::onOptionSelected,
        onCheck = viewModel::check,
        onRubricRatingChanged = viewModel::onRubricRatingChanged,
        onSubmitRubric = viewModel::submitProductionRubric,
        onNext = viewModel::next,
        onRetry = { viewModel.load(lessonId) },
        onSpeakCurrent = viewModel::speakCurrent,
        onSpeakWord = { article, german, audio -> viewModel.speakWord(article, german, audio) },
        onPlayListeningAudio = viewModel::playListeningAudio,
        onPlayDictationAudio = viewModel::playDictationAudio,
        onPlayModelAudio = viewModel::playModelAudio,
        onStartRecording = viewModel::startRecording,
        onStopRecording = viewModel::stopRecording,
        onPlayOwnRecording = viewModel::playOwnRecording,
        onRequestRecordPermission = {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (granted) viewModel.startRecording() else permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        },
        onFinished = viewModel::finish,
        uiLanguage = viewModel.uiLanguage.collectAsStateWithLifecycle().value,
    )
}

@Composable
internal fun LessonScreen(
    state: LessonUiState,
    onAnswerChanged: (String) -> Unit,
    onOptionSelected: (String) -> Unit,
    onCheck: () -> Unit,
    onRubricRatingChanged: (String, RubricRating) -> Unit,
    onSubmitRubric: () -> Unit,
    onNext: () -> Unit,
    onRetry: () -> Unit,
    onSpeakCurrent: () -> Unit,
    onSpeakWord: (String?, String, String?) -> Unit,
    onPlayListeningAudio: () -> Unit,
    onPlayDictationAudio: () -> Unit,
    onPlayModelAudio: (Float) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPlayOwnRecording: () -> Unit,
    onRequestRecordPermission: () -> Unit,
    onFinished: () -> Unit,
    uiLanguage: com.spreva.core.model.UiLanguage,
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
            onRubricRatingChanged = onRubricRatingChanged,
            onSubmitRubric = onSubmitRubric,
            onNext = onNext,
            onSpeakCurrent = onSpeakCurrent,
            onSpeakWord = onSpeakWord,
            onPlayListeningAudio = onPlayListeningAudio,
            onPlayDictationAudio = onPlayDictationAudio,
            onPlayModelAudio = onPlayModelAudio,
            onStartRecording = onStartRecording,
            onStopRecording = onStopRecording,
            onPlayOwnRecording = onPlayOwnRecording,
            onRequestRecordPermission = onRequestRecordPermission,
            onFinished = onFinished,
            uiLanguage = uiLanguage,
        )
    }
}

@Composable
private fun LessonContent(
    state: LessonUiState,
    onAnswerChanged: (String) -> Unit,
    onOptionSelected: (String) -> Unit,
    onCheck: () -> Unit,
    onRubricRatingChanged: (String, RubricRating) -> Unit,
    onSubmitRubric: () -> Unit,
    onNext: () -> Unit,
    onSpeakCurrent: () -> Unit,
    onSpeakWord: (String?, String, String?) -> Unit,
    onPlayListeningAudio: () -> Unit,
    onPlayDictationAudio: () -> Unit,
    onPlayModelAudio: (Float) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPlayOwnRecording: () -> Unit,
    onRequestRecordPermission: () -> Unit,
    onFinished: () -> Unit,
    uiLanguage: com.spreva.core.model.UiLanguage = com.spreva.core.model.UiLanguage.ENGLISH,
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
                    is LearningActivity.TextIntro -> TextIntroRenderer(current, uiLanguage)
                    is LearningActivity.VocabularyIntro -> VocabularyRenderer(
                        activity = current,
                        onSpeakWord = onSpeakWord,
                        uiLanguage = uiLanguage,
                    )
                    is LearningActivity.MultipleChoice -> MultipleChoiceRenderer(
                        activity = current,
                        selectedOptionId = state.selectedOptionId,
                        answerState = state.answerState,
                        onOptionSelected = onOptionSelected,
                        uiLanguage = uiLanguage,
                    )

                    is LearningActivity.Cloze -> ClozeRenderer(
                        activity = current,
                        typedAnswer = state.typedAnswer,
                        answerState = state.answerState,
                        onAnswerChanged = onAnswerChanged,
                    )

                    is LearningActivity.ListeningChoice -> ListeningChoiceRenderer(
                        activity = current,
                        selectedOptionId = state.selectedOptionId,
                        answerState = state.answerState,
                        onPlay = onPlayListeningAudio,
                        onOptionSelected = onOptionSelected,
                        uiLanguage = uiLanguage,
                    )

                    is LearningActivity.Dictation -> DictationRenderer(
                        activity = current,
                        typedAnswer = state.typedAnswer,
                        answerState = state.answerState,
                        onPlay = onPlayDictationAudio,
                        onAnswerChanged = onAnswerChanged,
                        uiLanguage = uiLanguage,
                    )

                    is LearningActivity.FreeWrite -> FreeWriteRenderer(
                        activity = current,
                        typedAnswer = state.typedAnswer,
                        answerState = state.answerState,
                        onAnswerChanged = onAnswerChanged,
                        uiLanguage = uiLanguage,
                    )

                    is LearningActivity.SpeakingPrompt -> SpeakingPromptRenderer(
                        activity = current,
                        state = state,
                        onStartRecording = onStartRecording,
                        onStopRecording = onStopRecording,
                        onPlayOwn = onPlayOwnRecording,
                        onRequestPermission = onRequestRecordPermission,
                        uiLanguage = uiLanguage,
                    )

                    is LearningActivity.SpeakingRepeat -> SpeakingRepeatRenderer(
                        activity = current,
                        state = state,
                        onPlayModel = onPlayModelAudio,
                        onStartRecording = onStartRecording,
                        onStopRecording = onStopRecording,
                        onPlayOwn = onPlayOwnRecording,
                        onRequestPermission = onRequestRecordPermission,
                        uiLanguage = uiLanguage,
                    )

                    is LearningActivity.LessonSummaryActivity -> SummaryRenderer(current, state, uiLanguage)
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

        val needsRubric = state.productionRubric != null &&
            state.answerState?.correct == true &&
            !state.rubricSubmitted

        if (state.productionRubric != null && state.answerState?.correct == true) {
            ProductionRubricPanel(
                state = state,
                onRatingChanged = onRubricRatingChanged,
                uiLanguage = uiLanguage,
            )
            Spacer(Modifier.height(12.dp))
        }

        val isSummary = activity is LearningActivity.LessonSummaryActivity
        val checkLabel = stringResource(R.string.spreva_lesson_check)
        val nextLabel = stringResource(R.string.spreva_lesson_next)
        val finishLabel = stringResource(R.string.spreva_lesson_finish)
        val rubricLabel = stringResource(R.string.spreva_lesson_save_self_review)

        Button(
            onClick = when {
                isSummary -> onFinished
                state.answerState == null -> onCheck
                needsRubric -> onSubmitRubric
                else -> onNext
            },
            enabled = when {
                isSummary -> true
                state.answerState == null -> state.canCheck
                needsRubric -> state.canSubmitRubric && !state.rubricSaving
                else -> true
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("lesson_primary_action"),
        ) {
            Text(
                when {
                    isSummary -> finishLabel
                    needsRubric -> if (state.rubricSaving) {
                        stringResource(R.string.spreva_lesson_saving_self_review)
                    } else {
                        rubricLabel
                    }
                    state.answerState != null -> nextLabel
                    else -> checkLabel
                },
            )
        }
    }
}

@Composable
private fun ProductionRubricPanel(
    state: LessonUiState,
    onRatingChanged: (String, RubricRating) -> Unit,
    uiLanguage: com.spreva.core.model.UiLanguage,
) {
    val rubric = state.productionRubric ?: return
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.spreva_lesson_self_review_rubric),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                stringResource(R.string.spreva_lesson_self_review_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            rubric.criteria.forEach { criterion ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        criterion.title.resolveFor(uiLanguage),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        criterion.guidance.resolveFor(uiLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    RubricRating.entries.forEach { rating ->
                        val selected = state.rubricRatings[criterion.id] == rating
                        Card(
                            onClick = {
                                if (!state.rubricSubmitted && !state.rubricSaving) {
                                    onRatingChanged(criterion.id, rating)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rubric_${criterion.id}_${rating.name}"),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = selected, onClick = null)
                                Text(rubricRatingLabel(rating))
                            }
                        }
                    }
                }
            }

            state.rubricScorePercent?.let { score ->
                Text(
                    stringResource(R.string.spreva_lesson_self_review_score, score),
                    style = MaterialTheme.typography.titleMedium,
                )
                LinearProgressIndicator(
                    progress = { score / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            state.rubricSaveError?.let {
                Text(
                    stringResource(R.string.spreva_lesson_self_review_save_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun rubricRatingLabel(rating: RubricRating): String = stringResource(
    when (rating) {
        RubricRating.NEEDS_REVISION -> R.string.spreva_rubric_needs_revision
        RubricRating.EMERGING -> R.string.spreva_rubric_emerging
        RubricRating.MOSTLY_EFFECTIVE -> R.string.spreva_rubric_mostly_effective
        RubricRating.CONSISTENT -> R.string.spreva_rubric_consistent
    },
)

@Composable
private fun SpeakingRepeatRenderer(
    activity: LearningActivity.SpeakingRepeat,
    state: LessonUiState,
    onPlayModel: (Float) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPlayOwn: () -> Unit,
    onRequestPermission: () -> Unit,
    uiLanguage: com.spreva.core.model.UiLanguage,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        activity.prompt?.let { prompt ->
            Text(prompt.resolveFor(uiLanguage), style = MaterialTheme.typography.titleMedium)
        }
        Text(activity.text.de, style = MaterialTheme.typography.headlineSmall)

        // 1) Listen to the native model at multiple speeds.
        Text(
            text = stringResource(R.string.spreva_lesson_shadowing_speed),
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            listOf(0.75f, 1.0f, 1.15f).forEach { speed ->
                OutlinedButton(
                    onClick = { onPlayModel(speed) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("${speed}×")
                }
            }
        }

        // 2) Record yourself (with runtime permission).
        state.recordingError?.let { error ->
            Text(
                text = stringResource(R.string.spreva_lesson_recording_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = { if (state.isRecording) onStopRecording() else onRequestPermission() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    if (state.isRecording) R.string.spreva_lesson_stop_recording
                    else R.string.spreva_lesson_start_recording,
                ),
            )
        }

        // 3) Compare: play your own recording back + automatic similarity.
        if (state.hasRecording) {
            OutlinedButton(onClick = onPlayOwn, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.spreva_lesson_play_own))
            }
            state.similarityScore?.let { score ->
                SimilarityBar(score)
            }
        }
        Text(
            text = stringResource(R.string.spreva_lesson_shadowing_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Phase 4.3: automatic waveform similarity readout (plan section 38 —
 * "compare" step of the shadowing loop). Neutral visual language: a bar,
 * never a judgmental grade — the learner judges themselves by ear first.
 */
@Composable
private fun SimilarityBar(score: Float) {
    val percent = (score * 100).toInt().coerceIn(0, 100)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.spreva_lesson_similarity, percent),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { score.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Big, selectable German text (plan sections 37/51). */
@Composable
private fun TextIntroRenderer(
    activity: LearningActivity.TextIntro,
    uiLanguage: com.spreva.core.model.UiLanguage,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(activity.title.resolveFor(uiLanguage), style = MaterialTheme.typography.headlineSmall)
        Text(activity.body.resolveFor(uiLanguage), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun VocabularyRenderer(
    activity: LearningActivity.VocabularyIntro,
    onSpeakWord: (String?, String, String?) -> Unit,
    uiLanguage: com.spreva.core.model.UiLanguage,
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
                            // Audit §9: instruction translation follows the UI
                            // language; the German word itself stays German.
                            text = word.translation.resolveFor(uiLanguage),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    // Phase 4.2: bundled native recording first, TTS fallback.
                    IconButton(onClick = { onSpeakWord(word.article, word.german, word.audio) }) {
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
    uiLanguage: com.spreva.core.model.UiLanguage,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = activity.prompt.resolveFor(uiLanguage),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
                    Text(option.text.resolveFor(uiLanguage), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun ListeningChoiceRenderer(
    activity: LearningActivity.ListeningChoice,
    selectedOptionId: String?,
    answerState: AnswerState?,
    onPlay: () -> Unit,
    onOptionSelected: (String) -> Unit,
    uiLanguage: com.spreva.core.model.UiLanguage,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        activity.prompt?.let { prompt ->
            Text(prompt.resolveFor(uiLanguage), style = MaterialTheme.typography.titleMedium)
        }
        // The audio IS the task: big play button, no text of the phrase (§32).
        Card(
            onClick = onPlay,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.VolumeUp,
                    contentDescription = stringResource(R.string.spreva_lesson_play_listening),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.spreva_lesson_play_listening),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
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
                    Text(option.text.resolveFor(uiLanguage), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun DictationRenderer(
    activity: LearningActivity.Dictation,
    typedAnswer: String,
    answerState: AnswerState?,
    onPlay: () -> Unit,
    onAnswerChanged: (String) -> Unit,
    uiLanguage: com.spreva.core.model.UiLanguage,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        activity.prompt?.let {
            Text(it.resolveFor(uiLanguage), style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = stringResource(R.string.spreva_lesson_dictation_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onPlay, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Filled.VolumeUp,
                contentDescription = stringResource(R.string.spreva_lesson_play_listening),
            )
            Spacer(Modifier.padding(4.dp))
            Text(stringResource(R.string.spreva_lesson_play_listening))
        }
        OutlinedTextField(
            value = typedAnswer,
            onValueChange = onAnswerChanged,
            enabled = answerState == null,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.spreva_lesson_type_answer)) },
        )
    }
}

@Composable
private fun FreeWriteRenderer(
    activity: LearningActivity.FreeWrite,
    typedAnswer: String,
    answerState: AnswerState?,
    onAnswerChanged: (String) -> Unit,
    uiLanguage: com.spreva.core.model.UiLanguage,
) {
    val words = typedAnswer.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(activity.prompt.resolveFor(uiLanguage), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.spreva_lesson_word_count, words, activity.minWords),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = typedAnswer,
            onValueChange = onAnswerChanged,
            enabled = answerState == null,
            modifier = Modifier.fillMaxWidth(),
            minLines = 6,
            label = { Text(stringResource(R.string.spreva_lesson_write_response)) },
        )
        if (activity.checklist.isNotEmpty()) {
            Text(stringResource(R.string.spreva_lesson_self_review), style = MaterialTheme.typography.titleSmall)
            activity.checklist.forEach { item ->
                Text("• ${item.resolveFor(uiLanguage)}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SpeakingPromptRenderer(
    activity: LearningActivity.SpeakingPrompt,
    state: LessonUiState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPlayOwn: () -> Unit,
    onRequestPermission: () -> Unit,
    uiLanguage: com.spreva.core.model.UiLanguage,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(activity.prompt.resolveFor(uiLanguage), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.spreva_lesson_speaking_target, activity.minSeconds),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (activity.checklist.isNotEmpty()) {
            Text(stringResource(R.string.spreva_lesson_self_review), style = MaterialTheme.typography.titleSmall)
            activity.checklist.forEach { item ->
                Text("• ${item.resolveFor(uiLanguage)}", style = MaterialTheme.typography.bodyMedium)
            }
        }
        state.recordingError?.let {
            Text(
                text = stringResource(R.string.spreva_lesson_recording_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = { if (state.isRecording) onStopRecording() else onRequestPermission() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    if (state.isRecording) R.string.spreva_lesson_stop_recording
                    else R.string.spreva_lesson_start_recording,
                ),
            )
        }
        state.recordingDurationMs?.let { duration ->
            Text(
                text = stringResource(R.string.spreva_lesson_recording_duration, duration / 1000L),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (state.hasRecording) {
            OutlinedButton(onClick = onPlayOwn, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.spreva_lesson_play_own))
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
private fun SummaryRenderer(
    activity: LearningActivity.LessonSummaryActivity,
    state: LessonUiState,
    uiLanguage: com.spreva.core.model.UiLanguage,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(activity.title.resolveFor(uiLanguage), style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(R.string.spreva_lesson_completed),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (state.objectiveAttempted > 0) {
            val percent = (state.objectiveCorrect * 100) / state.objectiveAttempted
            Card(Modifier.fillMaxWidth().testTag("summary_objective")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(
                            R.string.spreva_lesson_objective_score,
                            state.objectiveCorrect,
                            state.objectiveAttempted,
                            percent,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    LinearProgressIndicator(
                        progress = { percent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        if (state.productiveAttempted > 0) {
            Card(Modifier.fillMaxWidth().testTag("summary_productive")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(
                            R.string.spreva_lesson_productive_completion,
                            state.productiveCompleted,
                            state.productiveAttempted,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.spreva_lesson_productive_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
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
