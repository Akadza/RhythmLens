package com.rimuru.android.rhythmlens.ui.app.features.history

data class HistoryUiState(
    val isLoading: Boolean = false,
    val items: List<EcgHistoryItemUi> = emptyList(),
    val errorMessage: String? = null,
    val requiresPatientSelection: Boolean = false
)

sealed interface HistoryEvent {
    data object RetryClicked : HistoryEvent
    data class EcgClicked(val ecgId: String) : HistoryEvent
}

sealed interface HistoryEffect {
    data class NavigateToEcgDetail(val ecgId: String) : HistoryEffect
}
