package com.spreva.feature.review.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.spreva.core.model.ReviewRating

/**
 * Practice tab: simple summary + CTA (plan section 56 keeps review UI
 * minimal: prompt → recall → reveal → grade).
 */
@Composable
fun PracticeRoute(onStartReview: () -> Unit, viewModel: PracticeViewModel = hiltViewModel()) {
    val dueCount by viewModel.dueCount.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.spreva_review_practice_title),
            style = MaterialTheme.typography.headlineSmall,
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
                    GradeButtons(viewModel = viewModel)
                }
            } else if (!revealed) {
                Button(onClick = viewModel::reveal, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.spreva_review_show_answer))
                }
            } else {
                GradeButtons(viewModel = viewModel)
            }
        }
    }
}

/** Again/Hard/Good/Easy — extracted so both card flows share it. */
@Composable
private fun GradeButtons(viewModel: ReviewSessionViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { viewModel.grade(ReviewRating.AGAIN) },
            modifier = Modifier.weight(1f),
        ) { Text(stringResource(R.string.spreva_review_again)) }
        OutlinedButton(
            onClick = { viewModel.grade(ReviewRating.HARD) },
            modifier = Modifier.weight(1f),
        ) { Text(stringResource(R.string.spreva_review_hard)) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = { viewModel.grade(ReviewRating.GOOD) },
            modifier = Modifier.weight(1f),
        ) { Text(stringResource(R.string.spreva_review_good)) }
        Button(
            onClick = { viewModel.grade(ReviewRating.EASY) },
            modifier = Modifier.weight(1f),
        ) { Text(stringResource(R.string.spreva_review_easy)) }
    }
}
