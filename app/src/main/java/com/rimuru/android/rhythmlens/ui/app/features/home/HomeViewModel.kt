package com.rimuru.android.rhythmlens.ui.app.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(sampleInitialState())
    val uiState = _uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = _uiState.value
    )

    private val _effect = Channel<HomeEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.AddEcgClicked -> {
                _uiState.update { state ->
                    state.copy(isAddEcgSheetVisible = true)
                }
            }

            HomeEvent.AddEcgSheetDismissed -> {
                _uiState.update { state ->
                    state.copy(isAddEcgSheetVisible = false)
                }
            }

            HomeEvent.ScanClicked -> {
                closeSheet()
                _effect.trySend(HomeEffect.NavigateToScan)
            }

            HomeEvent.GalleryClicked -> {
                closeSheet()
                _effect.trySend(HomeEffect.OpenGalleryPicker)
            }

            HomeEvent.ImportClicked -> {
                closeSheet()
                _effect.trySend(HomeEffect.OpenFilePicker)
            }

            is HomeEvent.LastRecordClicked -> {
                _effect.trySend(HomeEffect.NavigateToEcgDetail(event.ecgId))
            }
        }
    }

    private fun closeSheet() {
        _uiState.update { state ->
            state.copy(isAddEcgSheetVisible = false)
        }
    }
}

private fun sampleInitialState(): HomeUiState {
    return HomeUiState(
        userName = "Александр",
        totalRecords = 3,
        linkedDoctorCount = 0,
        lastRecord = LastEcgUi(
            id = "ecg-1",
            date = "28.05.2026",
            mainResult = "Фибрилляция предсердий",
            probability = 50,
            digitizedLeads = 8,
            reconstructedLeads = 4
        )
    )
}
