package com.spreva.feature.onboarding.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spreva.core.model.LearningGoal
import com.spreva.core.model.ThemeMode
import com.spreva.core.model.UiLanguage
import com.spreva.feature.onboarding.impl.R

/**
 * Onboarding route: three quick steps (welcome → language → goal), then
 * completion is persisted and the user lands on Home with a lesson open
 * in under two minutes (plan sections 138-139).
 */
@Composable
fun OnboardingRoute(onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val step by viewModel.step.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val selectedGoal by viewModel.selectedGoal.collectAsStateWithLifecycle()

    OnboardingScreen(
        step = step,
        selectedLanguage = selectedLanguage,
        selectedGoal = selectedGoal,
        onSelectLanguage = viewModel::selectLanguage,
        onSelectGoal = viewModel::selectGoal,
        onNext = viewModel::next,
        onFinish = { viewModel.complete(onFinished) },
    )
}

@Composable
internal fun OnboardingScreen(
    step: OnboardingStep,
    selectedLanguage: UiLanguage?,
    selectedGoal: LearningGoal?,
    onSelectLanguage: (UiLanguage) -> Unit,
    onSelectGoal: (LearningGoal) -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        when (step) {
            OnboardingStep.WELCOME -> {
                Text(
                    text = stringResource(R.string.spreva_onboarding_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.spreva_onboarding_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(32.dp))
                Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.spreva_onboarding_start))
                }
            }

            OnboardingStep.LANGUAGE -> {
                Text(
                    text = stringResource(R.string.spreva_onboarding_language),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { onSelectLanguage(UiLanguage.ENGLISH) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (selectedLanguage == UiLanguage.ENGLISH) "✓ English" else "English",
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onSelectLanguage(UiLanguage.ARABIC) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (selectedLanguage == UiLanguage.ARABIC) "✓ العربية" else "العربية")
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onNext,
                    enabled = selectedLanguage != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.spreva_onboarding_continue))
                }
            }

            OnboardingStep.GOAL -> {
                Text(
                    text = stringResource(R.string.spreva_onboarding_goal),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(16.dp))
                // Localized labels (audit §7): raw enum ids never shown.
                listOf(
                    LearningGoal.ZERO_TO_C1 to R.string.spreva_onboarding_goal_zero,
                    LearningGoal.DAILY_LIFE to R.string.spreva_onboarding_goal_daily_life,
                    LearningGoal.CONVERSATION to R.string.spreva_onboarding_goal_conversation,
                    LearningGoal.WORK to R.string.spreva_onboarding_goal_work,
                    LearningGoal.EXAM to R.string.spreva_onboarding_goal_exam,
                ).forEach { (goal, labelRes) ->
                    OutlinedButton(
                        onClick = { onSelectGoal(goal) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (selectedGoal == goal) "✓ " + stringResource(labelRes)
                            else stringResource(labelRes),
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onFinish,
                    enabled = selectedGoal != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.spreva_onboarding_done))
                }
            }
        }
    }
}
