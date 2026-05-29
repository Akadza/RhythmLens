package com.rimuru.android.rhythmlens.ui.app.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimuru.android.rhythmlens.domain.model.EcgRecord
import com.rimuru.android.rhythmlens.domain.model.EcgStatus
import com.rimuru.android.rhythmlens.domain.usecase.GetEcgListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getEcgListUseCase: GetEcgListUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState())
    val uiState = _uiState.asStateFlow()

    private val _effect = Channel<HomeEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        observeDashboard()
    }

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

    private fun observeDashboard() {
        viewModelScope.launch {
            getEcgListUseCase(DEFAULT_PATIENT_ID)
                .catch {
                    _uiState.update { state ->
                        state.copy(
                            totalRecords = 0,
                            lastRecord = null
                        )
                    }
                }
                .collect { records ->
                    _uiState.update { state ->
                        val lastRecord = records.maxByOrNull { record ->
                            record.recordedAt
                        }

                        state.copy(
                            totalRecords = records.size,
                            lastRecord = lastRecord?.toLastEcgUi()
                        )
                    }
                }
        }
    }

    private fun closeSheet() {
        _uiState.update { state ->
            state.copy(isAddEcgSheetVisible = false)
        }
    }

    private fun EcgRecord.toLastEcgUi(): LastEcgUi {
        val digitizedLeads = digitizedSignal?.leads?.size ?: 0
        val reconstructedLeads = (STANDARD_LEAD_COUNT - digitizedLeads).coerceAtLeast(0)
        val statusText = processingMessage ?: status.toDisplayText()

        return LastEcgUi(
            id = id,
            date = DATE_FORMATTER.format(recordedAt.atZone(ZoneId.systemDefault())),
            mainResult = if (status == EcgStatus.ERROR) {
                errorMessage ?: statusText
            } else {
                statusText
            },
            probability = if (status == EcgStatus.PROCESSED) 0 else 0,
            digitizedLeads = digitizedLeads,
            reconstructedLeads = reconstructedLeads
        )
    }

    private fun EcgStatus.toDisplayText(): String {
        return when (this) {
            EcgStatus.DRAFT -> "Черновик"
            EcgStatus.UPLOADING -> "Загрузка изображения"
            EcgStatus.DIGITIZING -> "Оцифровка ЭКГ"
            EcgStatus.COMPLETING -> "Восстановление отведений"
            EcgStatus.ANALYZING -> "ИИ-анализ"
            EcgStatus.PROCESSED -> "Результат анализа"
            EcgStatus.ERROR -> "Ошибка обработки"
        }
    }

    private companion object {
        const val DEFAULT_PATIENT_ID = "temp-patient-id"
        const val STANDARD_LEAD_COUNT = 12
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }
}

private fun initialState(): HomeUiState {
    return HomeUiState(
        userName = "Александр",
        totalRecords = 0,
        linkedDoctorCount = 0,
        lastRecord = null
    )
}
