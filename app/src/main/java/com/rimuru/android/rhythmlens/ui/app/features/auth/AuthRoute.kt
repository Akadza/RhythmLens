package com.rimuru.android.rhythmlens.ui.app.features.auth

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AuthRoute(
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()

    AuthScreen(
        state = state.value,
        onEvent = viewModel::onEvent
    )
}
