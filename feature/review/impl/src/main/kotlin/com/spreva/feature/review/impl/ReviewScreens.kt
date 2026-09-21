package com.spreva.feature.review.impl

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spreva.core.model.CefrLevel
import com.spreva.core.model.MistakePattern
import com.spreva.core.model.ReviewRating
import com.spreva.core.model.SkillArea

/**
 * Practice tab: simple summary + CTA (plan section 56 keeps review UI
 * minimal: prompt → recall → reveal → grade).
 */
@Composable
fun PracticeRoute(
    onStartReview: () -> Unit,
    onStartPlacement: () -> Unit,
    onOpenLesson: (String) -> Unit,
    viewModel: PracticeViewModel = hiltViewModel(),
) {
    val dueCount by viewModel.dueCount.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val recommendedLevel by viewModel.recommendedLevel.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.spreva_review_practice_title),
            style = MaterialTheme.typography.headlineSmall,
        )

        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    stringResource(R.string.spreva_review_today_plan),
                    style = MaterialTheme.typography.titleLarge,
                )
                if (dueCount > 0) {
                    Text(stringResource(R.string.spreva_review_due_count, dueCount))
                    Button(onClick = onStartReview, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.spreva_review_start))
                    }
                } else {
                    Text(stringResource(R.string.spreva_review_none_due))
                    TextButton(onClick = onStartReview) {
                        Text(stringResource(R.string.spreva_review_check_anyway))
                    }
                }
                recommendedLevel?.let { level ->
                    Text(
                        stringResource(R.string.spreva_review_recommended_level, level.name),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                OutlinedButton(onClick = onStartPlacement, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.spreva_review_start_placement))
                }
            }
        }

        if (profile.mastery.isNotEmpty()) {
            Text(
                stringResource(R.string.spreva_review_mastery_title),
                style = MaterialTheme.typography.titleLarge,
            )
            profile.mastery.forEach { mastery ->
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(skillLabel(mastery.skill), style = MaterialTheme.typography.titleMedium)
                        val score = mastery.scorePercent
                        if (score != null) {
                            Text(
                                stringResource(
                                    R.string.spreva_review_mastery_score,
                                    score,
                                    mastery.confidencePercent,
                                ),
                            )
                            LinearProgressIndicator(
                                progress = { score / 100f },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Text(
                                stringResource(
                                    R.string.spreva_review_productive_evidence,
                                    mastery.productiveCompleted,
                                    mastery.productiveAttempts,
                                ),
                            )
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.spreva_review_mastery_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (profile.focus.isNotEmpty()) {
            Text(
                stringResource(R.string.spreva_review_focus_title),
                style = MaterialTheme.typography.titleLarge,
            )
            profile.focus.forEach { focus ->
                Card(
                    onClick = { onOpenLesson(focus.lessonId.value) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(focus.lessonTitle.de, style = MaterialTheme.typography.titleMedium)
                        val topic = profile.topics.firstOrNull { it.lessonId == focus.lessonId }
                        Text(
                            if (topic != null) {
                                stringResource(
                                    R.string.spreva_review_focus_detail_mastery,
                                    skillLabel(focus.skill),
                                    focus.priorityPercent,
                                    mistakeLabel(focus.reason),
                                    topic.scorePercent,
                                    topic.confidencePercent,
                                )
                            } else {
                                stringResource(
                                    R.string.spreva_review_focus_detail,
                                    skillLabel(focus.skill),
                                    focus.priorityPercent,
                                    mistakeLabel(focus.reason),
                                )
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        } else if (profile.totalAttempts > 0) {
            Text(
                stringResource(R.string.spreva_review_no_focus_needed),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** Review session: reveal then Again/Hard/Good/Easy. */
@Composable
fun ReviewSessionRoute(onFinished: () -> Unit, viewModel: ReviewSessionViewModel = hiltViewModel()) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val revealed by viewModel.revealed.collectAsStateWithLifecycle()
    val gradedCount by viewModel.gradedCount.collectAsStateWithLifecycle()
    val typedAnswer by viewModel.typedAnswer.collectAsStateWithLifecycle()
    val audioCheck by viewModel.audioCheck.collectAsStateWithLifecycle()

    val current = queue.dueCards.firstOrNull()
    val isAudioCard = current?.audioPath != null

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (current == null) {
            // Session done (or empty at start).
            Text(
                text = if (gradedCount > 0) {
                    stringResource(R.string.spreva_review_session_done)
                } else {
                    stringResource(R.string.spreva_review_none_due)
                },
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onFinished, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.spreva_review_back_to_practice))
            }
        } else {
            Text(
                text = "${gradedCount + 1}",
                style = MaterialTheme.typography.labelLarge,
            )
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (isAudioCard) {
                        // Phase 4.3 audio recall: hear it, type it. The prompt
                        // field is intentionally empty for these cards.
                        Button(onClick = { viewModel.playCardAudio(current) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.spreva_review_play_audio))
                        }
                        OutlinedTextField(
                            value = typedAnswer,
                            onValueChange = viewModel::onAnswerChanged,
                            enabled = audioCheck == null,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.spreva_review_type_heard)) },
                        )
                        when (audioCheck) {
                            true -> Text(
                                text = stringResource(R.string.spreva_review_correct),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            false -> Text(
                                text = stringResource(R.string.spreva_review_answer_was, current.answer),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            null -> Unit
                        }
                    } else {
                        Text(
                            text = current.prompt,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        if (revealed) {
                            Text(
                                text = current.answer,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (isAudioCard) {
                // Audio flow: check typed answer first, then self-grade.
                if (audioCheck == null) {
                    Button(
                        onClick = { current?.let(viewModel::checkAudioAnswer) },
                        enabled = typedAnswer.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.spreva_review_check))
                    }
                } else {
                    GradeButtons(viewModel = viewModel, card = current)
                }
            } else if (!revealed) {
                Button(onClick = viewModel::reveal, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.spreva_review_show_answer))
                }
            } else {
                GradeButtons(viewModel = viewModel, card = current)
            }
        }
    }
}

/** Again/Hard/Good/Easy with FSRS interval previews. */
@Composable
private fun GradeButtons(
    viewModel: ReviewSessionViewModel,
    card: com.spreva.core.model.ReviewCard,
) {
    val previews = viewModel.previewIntervals(card)

    @Composable
    fun label(rating: ReviewRating, textRes: Int): String {
        val base = stringResource(textRes)
        val interval = formatInterval(previews[rating] ?: 0L)
        return stringResource(R.string.spreva_review_rating_interval, base, interval)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { viewModel.grade(ReviewRating.AGAIN) },
            modifier = Modifier.weight(1f),
        ) { Text(label(ReviewRating.AGAIN, R.string.spreva_review_again)) }
        OutlinedButton(
            onClick = { viewModel.grade(ReviewRating.HARD) },
            modifier = Modifier.weight(1f),
        ) { Text(label(ReviewRating.HARD, R.string.spreva_review_hard)) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = { viewModel.grade(ReviewRating.GOOD) },
            modifier = Modifier.weight(1f),
        ) { Text(label(ReviewRating.GOOD, R.string.spreva_review_good)) }
        Button(
            onClick = { viewModel.grade(ReviewRating.EASY) },
            modifier = Modifier.weight(1f),
        ) { Text(label(ReviewRating.EASY, R.string.spreva_review_easy)) }
    }
}

@Composable
private fun formatInterval(durationMs: Long): String {
    val minutes = (durationMs / 60_000L).coerceAtLeast(1L)
    return when {
        minutes < 60 -> stringResource(R.string.spreva_review_minutes, minutes)
        minutes < 60 * 24 -> stringResource(R.string.spreva_review_hours, minutes / 60)
        else -> stringResource(R.string.spreva_review_days, minutes / (60 * 24))
    }
}

@Composable
fun PlacementRoute(
    onOpenCourse: (String) -> Unit,
    onFinished: () -> Unit,
    viewModel: PlacementViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.spreva_placement_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            stringResource(R.string.spreva_placement_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when {
            state.isLoading -> Text(stringResource(R.string.spreva_placement_loading))
            state.error != null -> {
                Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = onFinished, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.spreva_review_back_to_practice))
                }
            }
            state.result?.finished == true -> {
                val result = state.result
                val level = result?.recommendedLevel ?: CefrLevel.B1
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            stringResource(R.string.spreva_placement_result, level.name),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            stringResource(
                                R.string.spreva_placement_confidence,
                                result?.confidencePercent ?: 0,
                                result?.answeredCount ?: 0,
                            ),
                        )
                        Text(
                            stringResource(R.string.spreva_placement_result_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Button(
                    onClick = { onOpenCourse("de-core-${level.name.lowercase()}") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.spreva_placement_open_level, level.name))
                }
                OutlinedButton(onClick = viewModel::restart, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.spreva_placement_restart))
                }
                TextButton(onClick = onFinished, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.spreva_review_back_to_practice))
                }
            }
            else -> {
                val question = state.currentQuestion
                if (question != null) {
                    val answered = state.history.size
                    Text(
                        stringResource(
                            R.string.spreva_placement_progress,
                            answered + 1,
                            state.currentLevel.name,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                question.prompt.de,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(question.question.de, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    question.options.forEach { option ->
                        val selected = state.selectedOptionId == option.id
                        Card(
                            onClick = { viewModel.selectOption(option.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = selected, onClick = null)
                                Text(option.text.de)
                            }
                        }
                    }
                    Button(
                        onClick = viewModel::submit,
                        enabled = state.selectedOptionId != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.spreva_placement_next))
                    }
                }
            }
        }
    }
}

@Composable
private fun skillLabel(skill: SkillArea): String = stringResource(
    when (skill) {
        SkillArea.GRAMMAR -> R.string.spreva_skill_grammar
        SkillArea.READING -> R.string.spreva_skill_reading
        SkillArea.LISTENING -> R.string.spreva_skill_listening
        SkillArea.WRITING -> R.string.spreva_skill_writing
        SkillArea.SPEAKING -> R.string.spreva_skill_speaking
        SkillArea.PRONUNCIATION -> R.string.spreva_skill_pronunciation
    },
)

@Composable
private fun mistakeLabel(pattern: MistakePattern): String = stringResource(
    when (pattern) {
        MistakePattern.REPEATED_ERROR -> R.string.spreva_mistake_repeated
        MistakePattern.REPAIRED_AFTER_ERROR -> R.string.spreva_mistake_repaired
        MistakePattern.RUSHED_GUESS -> R.string.spreva_mistake_rushed
        MistakePattern.SLOW_RECALL -> R.string.spreva_mistake_slow
        MistakePattern.PRODUCTIVE_INCOMPLETE -> R.string.spreva_mistake_productive
    },
)
