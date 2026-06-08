package com.rimuru.android.rhythmlens.ui.app.features.comparison

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ComparisonRoute(
    onNavigateBack: () -> Unit,
    viewModel: ComparisonViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ComparisonEffect.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    ComparisonScreen(
        state = state.value,
        onEvent = viewModel::onEvent
    )
}
