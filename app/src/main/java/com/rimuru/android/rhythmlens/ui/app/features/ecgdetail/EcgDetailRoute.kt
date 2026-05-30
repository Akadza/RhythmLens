package com.rimuru.android.rhythmlens.ui.app.features.ecgdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EcgDetailRoute(
    onNavigateBack: () -> Unit,
    onNavigateToComparison: (String) -> Unit,
    onNavigateToSyntheticImage: (String) -> Unit,
    onNavigateToExport: (String) -> Unit,
    viewModel: EcgDetailViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                EcgDetailEffect.NavigateBack -> {
                    onNavigateBack()
                }

                is EcgDetailEffect.NavigateToComparison -> {
                    onNavigateToComparison(effect.ecgId)
                }

                is EcgDetailEffect.NavigateToSyntheticImage -> {
                    onNavigateToSyntheticImage(effect.ecgId)
                }

                is EcgDetailEffect.NavigateToExport -> {
                    onNavigateToExport(effect.ecgId)
                }
            }
        }
    }

    EcgDetailScreen(
        state = state.value,
        onEvent = viewModel::onEvent
    )
}
