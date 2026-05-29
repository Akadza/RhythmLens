package com.rimuru.android.rhythmlens.ui.app.features.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimuru.android.rhythmlens.domain.model.EcgRecord
import com.rimuru.android.rhythmlens.domain.model.EcgStatus
import com.rimuru.android.rhythmlens.domain.usecase.GetEcgListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getEcgListUseCase: GetEcgListUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState(isLoading = true))
    val uiState = _uiState

    private val _effect = Channel<HistoryEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadHistory()
    }

    fun onEvent(event: HistoryEvent) {
        when (event) {
            HistoryEvent.RetryClicked -> {
                loadHistory()
            }

            is HistoryEvent.EcgClicked -> {
                _effect.trySend(HistoryEffect.NavigateToEcgDetail(event.ecgId))
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isLoading = true, errorMessage = null)
            }

            getEcgListUseCase(DEFAULT_PATIENT_ID)
                .catch { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Не удалось загрузить историю ЭКГ"
                        )
                    }
                }
                .collect { records ->
                    _uiState.update {
                        HistoryUiState(
                            isLoading = false,
                            items = records.map { record -> record.toUi() },
                            errorMessage = null
                        )
                    }
                }
        }
    }

    private fun EcgRecord.toUi(): EcgHistoryItemUi {
        return EcgHistoryItemUi(
            id = id,
            date = DATE_FORMATTER.format(recordedAt.atZone(ZoneId.systemDefault())),
            patientName = patientId,
            mainResult = when (status) {
                EcgStatus.PROCESSED -> "Результат анализа"
                EcgStatus.PENDING -> "Ожидает обработки"
                EcgStatus.ERROR -> "Ошибка обработки"
            },
            probability = 0,
            digitizedLeads = digitizedSignal?.leads?.size ?: 0,
            reconstructedLeads = 0,
            status = when (status) {
                EcgStatus.PROCESSED -> EcgProcessingStatusUi.Processed
                EcgStatus.PENDING -> EcgProcessingStatusUi.Processing
                EcgStatus.ERROR -> EcgProcessingStatusUi.Error
            }
        )
    }

    private companion object {
        const val DEFAULT_PATIENT_ID = "temp-patient-id"
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }
}
