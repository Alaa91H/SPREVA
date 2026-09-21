package com.spreva.feature.course.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spreva.core.model.LessonStatus

/**
 * Learn tab: clean vertical CEFR journey (plan section 47).
 * All bundled CEFR levels are rendered from content data.
 */
@Composable
fun LearnRoute(onOpenCourse: (levelId: String) -> Unit, viewModel: LearnViewModel = hiltViewModel()) {
    val course by viewModel.courseState.collectAsStateWithLifecycle()
    val progress by viewModel.progressByLesson.collectAsStateWithLifecycle()
    val uiLanguage by viewModel.uiLanguage.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Learn German",
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        val levels = course?.levels.orEmpty()
        if (levels.isEmpty()) {
            item { Text("Loading...") }
        }
        items(levels, key = { it.id.value }) { level ->
            val lessons = level.units.flatMap { it.lessons }
            val completed = lessons.count { progress[it.id.value]?.status == LessonStatus.COMPLETED }
            val percent = if (lessons.isEmpty()) 0 else (completed * 100) / lessons.size
            Card(
                onClick = { onOpenCourse(level.id.value) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(level.title.resolveFor(uiLanguage), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "$completed / ${lessons.size}",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { percent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** Level screen listing units and lessons with per-lesson status. */
@Composable
fun CourseRoute(
    levelId: String,
    onOpenLesson: (lessonId: String) -> Unit,
    viewModel: CourseViewModel = hiltViewModel(),
) {
    val course by viewModel.courseState.collectAsStateWithLifecycle()
    val progress by viewModel.progressByLesson.collectAsStateWithLifecycle()
    val uiLanguage by viewModel.uiLanguage.collectAsStateWithLifecycle()

    LaunchedEffect(levelId) {
        viewModel.setLevel(levelId)
    }

    val level = course?.levels?.firstOrNull { it.id.value == levelId }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = level?.title?.resolveFor(uiLanguage) ?: "Loading...",
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        level?.units?.forEach { unit ->
            item(key = "unit_${unit.id.value}") {
                Text(
                    text = unit.title.resolveFor(uiLanguage),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(unit.lessons, key = { "lesson_${it.id.value}" }) { lesson ->
                val status = progress[lesson.id.value]?.status
                val done = status == LessonStatus.COMPLETED
                Card(
                    onClick = { onOpenLesson(lesson.id.value) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = lesson.title.resolveFor(uiLanguage).ifBlank { lesson.id.value },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${lesson.activityCount} activities",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        Text(
                            text = if (done) "✓" else if (status == LessonStatus.IN_PROGRESS) "…" else "",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
            }
        }
    }
}
