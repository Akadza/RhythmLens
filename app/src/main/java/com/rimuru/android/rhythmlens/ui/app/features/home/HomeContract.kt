package com.rimuru.android.rhythmlens.ui.app.features.home

data class HomeUiState(
    val userName: String = "",
    val selectedPatientId: String? = null,
    val totalRecords: Int = 0,
    val linkedDoctorCount: Int = 0,
    val lastRecord: LastEcgUi? = null,
    val isAddEcgSheetVisible: Boolean = false,
    val isCreatingTestEcg: Boolean = false
)

data class LastEcgUi(
    val id: String,
    val date: String,
    val mainResult: String,
    val probability: Int?,
    val digitizedLeads: Int,
    val reconstructedLeads: Int
)

sealed interface HomeEvent {
    data object AddEcgClicked : HomeEvent
    data object AddEcgSheetDismissed : HomeEvent
    data object ScanClicked : HomeEvent
    data object GalleryClicked : HomeEvent
    data object ImportClicked : HomeEvent
    data object CreateTestEcgClicked : HomeEvent
    data class LastRecordClicked(val ecgId: String) : HomeEvent
}

sealed interface HomeEffect {
    data object OpenCamera : HomeEffect
    data object OpenGalleryPicker : HomeEffect
    data object OpenFilePicker : HomeEffect
    data class NavigateToEcgDetail(val ecgId: String) : HomeEffect
}
