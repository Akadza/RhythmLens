package com.rimuru.android.rhythmlens.ui.app.features.syntheticimage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SyntheticImageRoute(
    onNavigateBack: () -> Unit,
    viewModel: SyntheticImageViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SyntheticImageEffect.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    SyntheticImageScreen(
        state = state.value,
        onEvent = viewModel::onEvent
    )
}
