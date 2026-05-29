package com.rimuru.android.rhythmlens.ui.app.features.ecgdetail

sealed interface EcgDetailEvent {
    data object BackClicked : EcgDetailEvent
    data object CompareClicked : EcgDetailEvent
    data object SyntheticClicked : EcgDetailEvent
    data object ExportClicked : EcgDetailEvent
    data object DoctorConclusionClicked : EcgDetailEvent
    data object DeleteClicked : EcgDetailEvent
}

sealed interface EcgDetailEffect {
    data object NavigateBack : EcgDetailEffect
    data class NavigateToComparison(val ecgId: String) : EcgDetailEffect
    data class NavigateToSyntheticImage(val ecgId: String) : EcgDetailEffect
    data class NavigateToExport(val ecgId: String) : EcgDetailEffect
    data class OpenDoctorConclusion(val ecgId: String) : EcgDetailEffect
    data class ConfirmDelete(val ecgId: String) : EcgDetailEffect
}
