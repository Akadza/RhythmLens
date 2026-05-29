package com.rimuru.android.rhythmlens.ui.app.features.ecgdetail

import com.rimuru.android.rhythmlens.domain.model.EcgPoint
import com.rimuru.android.rhythmlens.ui.app.features.ecgdetail.components.DiagnosisProbabilityUi

sealed interface EcgDetailEvent {
    data object BackClicked : EcgDetailEvent
    data object CompareClicked : EcgDetailEvent
    data object SyntheticClicked : EcgDetailEvent
    data object ExportClicked : EcgDetailEvent
    data object DoctorConclusionClicked : EcgDetailEvent
    data object DeleteClicked : EcgDetailEvent
    data object DeleteConfirmed : EcgDetailEvent
    data object DeleteDismissed : EcgDetailEvent
    data class SignalModeChanged(val mode: SignalModeUi) : EcgDetailEvent
}

sealed interface EcgDetailEffect {
    data object NavigateBack : EcgDetailEffect
    data class NavigateToComparison(val ecgId: String) : EcgDetailEffect
    data class NavigateToSyntheticImage(val ecgId: String) : EcgDetailEffect
    data class NavigateToExport(val ecgId: String) : EcgDetailEffect
    data class OpenDoctorConclusion(val ecgId: String) : EcgDetailEffect
}

data class EcgDetailUiState(
    val ecgId: String,
    val date: String,
    val probabilities: List<DiagnosisProbabilityUi>,
    val signalInfo: SignalInfoUi,
    val leads: List<LeadSummaryUi>,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val signalMode: SignalModeUi = SignalModeUi.Full,
    val isDeleteDialogVisible: Boolean = false,
    val isDeleting: Boolean = false
)

data class SignalInfoUi(
    val duration: String,
    val samplingRate: String,
    val digitizedLeads: Int,
    val reconstructedLeads: Int,
    val source: String,
    val quality: String
)

data class LeadSummaryUi(
    val name: String,
    val origin: LeadOriginUi,
    val points: List<EcgPoint> = emptyList()
)

enum class LeadOriginUi {
    Digitized,
    Reconstructed,
    Mixed
}

enum class SignalModeUi {
    Full,
    DigitizedOnly
}
