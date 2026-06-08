package com.rimuru.android.rhythmlens.ui.theme

import androidx.compose.runtime.compositionLocalOf

data class RhythmThemeController(
    val isDarkTheme: Boolean,
    val onToggleTheme: () -> Unit
)

val LocalRhythmThemeController = compositionLocalOf<RhythmThemeController?> { null }
