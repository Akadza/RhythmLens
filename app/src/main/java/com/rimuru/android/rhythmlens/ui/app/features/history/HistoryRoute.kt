package com.rimuru.android.rhythmlens.ui.app.features.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HistoryRoute(
    onNavigateToEcgDetail: (String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HistoryEffect.NavigateToEcgDetail -> {
                    onNavigateToEcgDetail(effect.ecgId)
                }
            }
        }
    }

    HistoryScreen(
        state = state.value,
        onEvent = viewModel::onEvent
    )
}
