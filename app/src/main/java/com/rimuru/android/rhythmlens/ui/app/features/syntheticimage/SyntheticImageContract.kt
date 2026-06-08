package com.rimuru.android.rhythmlens.ui.app.features.syntheticimage

data class SyntheticImageUiState(
    val ecgId: String = "",
    val imageUri: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface SyntheticImageEvent {
    data object BackClicked : SyntheticImageEvent
    data object RetryClicked : SyntheticImageEvent
}

sealed interface SyntheticImageEffect {
    data object NavigateBack : SyntheticImageEffect
}
