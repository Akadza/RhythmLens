package com.rimuru.android.rhythmlens.ui.app.features.comparison

import com.rimuru.android.rhythmlens.domain.model.EcgLeadSegment
import com.rimuru.android.rhythmlens.domain.model.EcgPoint

data class ComparisonUiState(
    val baseEcgId: String = "",
    val baseTitle: String = "",
    val selectedComparedEcgId: String? = null,
    val candidates: List<ComparisonCandidateUi> = emptyList(),
    val leads: List<ComparisonLeadUi> = emptyList(),
    val summary: ComparisonSummaryUi = ComparisonSummaryUi.Empty,
    val signalMode: ComparisonSignalModeUi = ComparisonSignalModeUi.Full,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class ComparisonCandidateUi(
    val id: String,
    val title: String,
    val subtitle: String
)

data class ComparisonLeadUi(
    val name: String,
    val basePoints: List<EcgPoint>,
    val comparedPoints: List<EcgPoint>,
    val baseSegments: List<EcgLeadSegment>,
    val comparedSegments: List<EcgLeadSegment>
)

data class ComparisonSummaryUi(
    val title: String,
    val points: List<String>
) {
    companion object {
        val Empty = ComparisonSummaryUi(
            title = "",
            points = emptyList()
        )
    }
}

enum class ComparisonSignalModeUi {
    Full,
    DigitizedOnly
}

sealed interface ComparisonEvent {
    data object BackClicked : ComparisonEvent
    data class ComparedRecordSelected(val ecgId: String) : ComparisonEvent
    data class SignalModeChanged(val mode: ComparisonSignalModeUi) : ComparisonEvent
}

sealed interface ComparisonEffect {
    data object NavigateBack : ComparisonEffect
}
