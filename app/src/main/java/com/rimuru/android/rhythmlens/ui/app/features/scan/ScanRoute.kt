package com.rimuru.android.rhythmlens.ui.app.features.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ScanRoute(
    onNavigateBack: () -> Unit,
    onNavigateToEcgDetail: (String) -> Unit,
    onOpenCamera: () -> Unit,
    onOpenGalleryPicker: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ScanEffect.NavigateBack -> {
                    onNavigateBack()
                }

                ScanEffect.OpenCamera -> {
                    onOpenCamera()
                }

                ScanEffect.OpenGalleryPicker -> {
                    onOpenGalleryPicker()
                }

                is ScanEffect.NavigateToEcgDetail -> {
                    onNavigateToEcgDetail(effect.ecgId)
                }
            }
        }
    }

    ScanScreen(
        state = state.value,
        onEvent = viewModel::onEvent
    )
}
