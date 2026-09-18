package com.spreva.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spreva.core.model.UiLanguage
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity host (plan section 3). Edge-to-edge from the first
 * frame; Window Size Class drives the adaptive shell (plan sections 21-26).
 * Extends AppCompatActivity so per-app locales (Arabic UI) apply
 * immediately (plan section 213).
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: MainViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            // Persisted locale applied at composition root.
            LaunchedEffect(settings.uiLanguage) {
                val tag = when (settings.uiLanguage) {
                    UiLanguage.ENGLISH -> "en"
                    UiLanguage.ARABIC -> "ar"
                }
                if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != tag) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                }
            }

            val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass

            SprevaApp(
                settings = settings,
                widthSizeClass = widthSizeClass,
            )
        }
    }
}
