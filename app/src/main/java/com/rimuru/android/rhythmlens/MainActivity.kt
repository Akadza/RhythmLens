package com.rimuru.android.rhythmlens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rimuru.android.rhythmlens.ui.RhythmLensApp
import com.rimuru.android.rhythmlens.ui.theme.LocalRhythmThemeController
import com.rimuru.android.rhythmlens.ui.theme.RhythmLensTheme
import com.rimuru.android.rhythmlens.ui.theme.RhythmThemeController
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val systemDarkTheme = isSystemInDarkTheme()
            var isDarkTheme by rememberSaveable {
                mutableStateOf(systemDarkTheme)
            }

            RhythmLensTheme(darkTheme = isDarkTheme) {
                CompositionLocalProvider(
                    LocalRhythmThemeController provides RhythmThemeController(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = {
                            isDarkTheme = !isDarkTheme
                        }
                    )
                ) {
                    RhythmLensApp()
                }
            }
        }
    }
}
