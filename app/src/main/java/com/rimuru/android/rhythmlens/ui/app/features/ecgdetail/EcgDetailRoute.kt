package com.rimuru.android.rhythmlens.ui.app.features.ecgdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun EcgDetailRoute(
    onNavigateBack: () -> Unit,
    onNavigateToComparison: (String) -> Unit,
    onNavigateToSyntheticImage: (String) -> Unit,
    onNavigateToExport: (String) -> Unit,
    onOpenDoctorConclusion: (String) -> Unit,
    onConfirmDelete: (String) -> Unit,
    viewModel: EcgDetailViewModel = hiltViewModel()
) {
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

                is EcgDetailEffect.OpenDoctorConclusion -> {
                    onOpenDoctorConclusion(effect.ecgId)
                }

                is EcgDetailEffect.ConfirmDelete -> {
                    onConfirmDelete(effect.ecgId)
                }
            }
        }
    }

    EcgDetailScreen(
        ecgId = viewModel.ecgId,
        onBackClick = {
            viewModel.onEvent(EcgDetailEvent.BackClicked)
        },
        onCompareClick = {
            viewModel.onEvent(EcgDetailEvent.CompareClicked)
        },
        onSyntheticClick = {
            viewModel.onEvent(EcgDetailEvent.SyntheticClicked)
        },
        onExportClick = {
            viewModel.onEvent(EcgDetailEvent.ExportClicked)
        }
    )
}
