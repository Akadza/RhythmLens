package com.rimuru.android.rhythmlens.ui.app.features.profile

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProfileRoute(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(
        state = state.value,
        onEvent = viewModel::onEvent
    )
}
