package com.rimuru.android.rhythmlens.ui.app.features.scan

data class ScanUiState(
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)

sealed interface ScanEvent {
    data object BackClicked : ScanEvent
    data object TakePhotoClicked : ScanEvent
    data object ChooseFromGalleryClicked : ScanEvent
    data object CreateTestEcgClicked : ScanEvent
}

sealed interface ScanEffect {
    data object NavigateBack : ScanEffect
    data object OpenCamera : ScanEffect
    data object OpenGalleryPicker : ScanEffect
    data class NavigateToEcgDetail(val ecgId: String) : ScanEffect
}
