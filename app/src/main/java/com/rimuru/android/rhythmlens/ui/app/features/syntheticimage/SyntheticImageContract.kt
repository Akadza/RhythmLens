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
    data object SaveClicked : SyntheticImageEvent
    data object ShareClicked : SyntheticImageEvent
}

sealed interface SyntheticImageEffect {
    data object NavigateBack : SyntheticImageEffect
    data class SaveImage(val imageUri: String) : SyntheticImageEffect
    data class ShareImage(val imageUri: String) : SyntheticImageEffect
}
