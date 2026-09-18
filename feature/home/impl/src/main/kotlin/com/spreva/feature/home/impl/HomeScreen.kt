package com.spreva.feature.home.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Home: one clear primary action ("ماذا أفعل الآن؟" in two seconds,
 * plan sections 44-46). Shows the Daily Coach card with the next lesson
 * plus the current due-review count.
 */
@Composable
fun HomeRoute(
    onOpenLearn: () -> Unit,
    onStartReview: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val progress by viewModel.allProgress.collectAsStateWithLifecycle()

    HomeScreen(
        state = state,
        completedLessons = progress.count { it.status == com.spreva.core.model.LessonStatus.COMPLETED },
        onContinueLesson = onOpenLearn,
        onStartReview = onStartReview,
    )
}

@Composable
internal fun HomeScreen(
    state: HomeUiState,
    completedLessons: Int,
    onContinueLesson: () -> Unit,
    onStartReview: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = stringResource(R.string.spreva_home_greeting), style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(R.string.spreva_home_level),
            style = MaterialTheme.typography.bodyMedium,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.spreva_home_daily_coach),
                    style = MaterialTheme.typography.titleMedium,
                )
                LinearProgressIndicator(
                    progress = {
                        if (state.isLoading) 0f else 0.1f * (completedLessons + 1).coerceAtMost(10)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = stringResource(R.string.spreva_home_continue_lesson))
                    Button(
                        onClick = onContinueLesson,
                        enabled = !state.isLoading && state.nextLessonId != null,
                    ) {
                        Text(stringResource(R.string.spreva_home_start))
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.spreva_home_review),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (state.dueReviews > 0) {
                    Text(
                        text = stringResource(R.string.spreva_home_due_reviews, state.dueReviews),
                    )
                    TextButton(onClick = onStartReview) {
                        Text(stringResource(R.string.spreva_home_start))
                    }
                } else {
                    Text(text = stringResource(R.string.spreva_home_no_reviews))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.spreva_home_lessons_completed, completedLessons),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
