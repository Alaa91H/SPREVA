package com.spreva.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.spreva.app.navigation.CourseKey
import com.spreva.app.navigation.LessonKey
import com.spreva.app.navigation.ReviewSessionKey
import com.spreva.app.navigation.WelcomeKey
import com.spreva.core.designsystem.theme.SprevaTheme
import com.spreva.core.model.UserSettings
import com.spreva.core.navigation.HomeKey
import com.spreva.core.navigation.LearnKey
import com.spreva.core.navigation.PracticeKey
import com.spreva.core.navigation.SettingsKey

/**
 * App shell: Navigation 3 [NavDisplay] + adaptive scaffold. Phone (compact)
 * gets a bottom NavigationBar, medium/expanded widths get a NavigationRail
 * (plan sections 20-26). The start destination is Welcome until onboarding
 * completes, then Home (plan section 110: no login gate before first lesson).
 */
@Composable
fun SprevaApp(
    settings: UserSettings,
    widthSizeClass: WindowWidthSizeClass,
) {
    val backStack = rememberNavBackStack(WelcomeKey)

    SprevaTheme(themeMode = settings.themeMode, useDynamicColor = settings.useDynamicColor) {
        val showRail = widthSizeClass != WindowWidthSizeClass.Compact
        val current = backStack.lastOrNull()

        // Tab selection resets the stack to the chosen top-level destination.
        val selectTab: (NavKey) -> Unit = { key ->
            backStack.clear()
            backStack.add(key)
        }

        Scaffold(
            bottomBar = {
                if (!showRail && current in topLevelKeys()) {
                    SprevaBottomBar(current = current, onSelectTab = selectTab)
                }
            },
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize()) {
                if (showRail && current in topLevelKeys()) {
                    SprevaNavRail(current = current, onSelectTab = selectTab)
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    NavDisplay(
                        backStack = backStack,
                        modifier = Modifier.fillMaxSize(),
                        onBack = { backStack.removeLastOrNull() },
                        entryProvider = sprevaEntryProvider(
                            onOpenLearn = { backStack.add(LearnKey) },
                            onOpenReview = { backStack.add(ReviewSessionKey) },
                            onOpenCourse = { levelId -> backStack.add(CourseKey(levelId)) },
                            onOpenLesson = { lessonId -> backStack.add(LessonKey(lessonId)) },
                            onBack = { backStack.removeLastOrNull() },
                            onGoHome = {
                                backStack.clear()
                                backStack.add(HomeKey)
                            },
                        ),
                    )
                }
            }
        }
    }
}

/** Detail destinations (welcome/course/lesson/review) hide the nav suite. */
private fun topLevelKeys(): List<NavKey> = listOf(HomeKey, LearnKey, PracticeKey, SettingsKey)

@Composable
private fun SprevaBottomBar(current: NavKey?, onSelectTab: (NavKey) -> Unit) {
    NavigationBar {
        topLevelDestinations().forEach { destination ->
            NavigationBarItem(
                selected = current == destination.key,
                onClick = { onSelectTab(destination.key) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(stringResource(destination.labelRes)) },
            )
        }
    }
}

@Composable
private fun SprevaNavRail(current: NavKey?, onSelectTab: (NavKey) -> Unit) {
    NavigationRail {
        topLevelDestinations().forEach { destination ->
            NavigationRailItem(
                selected = current == destination.key,
                onClick = { onSelectTab(destination.key) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(stringResource(destination.labelRes)) },
            )
        }
    }
}

/** Minimal data holder for top-level tabs. */
private data class TopLevelDestination(
    val key: NavKey,
    val icon: ImageVector,
    @StringRes val labelRes: Int,
)

private fun topLevelDestinations(): List<TopLevelDestination> = listOf(
    TopLevelDestination(HomeKey, Icons.Filled.Home, R.string.dest_home),
    TopLevelDestination(LearnKey, Icons.Filled.Star, R.string.dest_learn),
    TopLevelDestination(PracticeKey, Icons.Filled.ThumbUp, R.string.dest_practice),
    TopLevelDestination(SettingsKey, Icons.Filled.Settings, R.string.dest_settings),
)
