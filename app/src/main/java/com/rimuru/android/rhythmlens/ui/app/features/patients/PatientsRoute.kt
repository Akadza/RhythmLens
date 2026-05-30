package com.rimuru.android.rhythmlens.ui.app.features.patients

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PatientsRoute(
    viewModel: PatientsViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()

    PatientsScreen(
        state = state.value,
        onEvent = viewModel::onEvent
    )
}
