package com.rimuru.android.rhythmlens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rimuru.android.rhythmlens.ui.RhythmLensApp
import com.rimuru.android.rhythmlens.ui.theme.RhythmLensTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RhythmLensTheme {
                RhythmLensApp()
            }
        }
    }
}