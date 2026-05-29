package com.rimuru.android.rhythmlens.ui.app.features.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HomeRoute(
    onOpenCamera: () -> Unit,
    onOpenGalleryPicker: () -> Unit,
    onOpenFilePicker: () -> Unit,
    onNavigateToEcgDetail: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                HomeEffect.OpenCamera -> {
                    onOpenCamera()
                }

                HomeEffect.OpenGalleryPicker -> {
                    onOpenGalleryPicker()
                }

                HomeEffect.OpenFilePicker -> {
                    onOpenFilePicker()
                }

                is HomeEffect.NavigateToEcgDetail -> {
                    onNavigateToEcgDetail(effect.ecgId)
                }
            }
        }
    }

    HomeScreen(
        state = state.value,
        onEvent = viewModel::onEvent
    )
}
