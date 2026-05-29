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
        val statusText = processingMessage ?: status.toDisplayText()

        return EcgHistoryItemUi(
            id = id,
            date = DATE_FORMATTER.format(recordedAt.atZone(ZoneId.systemDefault())),
            patientName = patientId,
            mainResult = if (status == EcgStatus.ERROR) {
                errorMessage ?: statusText
            } else {
                statusText
            },
            probability = 0,
            digitizedLeads = digitizedSignal?.leads?.size ?: 0,
            reconstructedLeads = 0,
            status = status.toUiStatus()
        )
    }

    private fun EcgStatus.toUiStatus(): EcgProcessingStatusUi {
        return when (this) {
            EcgStatus.DRAFT -> EcgProcessingStatusUi.Draft
            EcgStatus.UPLOADING -> EcgProcessingStatusUi.Uploading
            EcgStatus.DIGITIZING -> EcgProcessingStatusUi.Digitizing
            EcgStatus.COMPLETING -> EcgProcessingStatusUi.Completing
            EcgStatus.ANALYZING -> EcgProcessingStatusUi.Analyzing
            EcgStatus.PROCESSED -> EcgProcessingStatusUi.Processed
            EcgStatus.ERROR -> EcgProcessingStatusUi.Error
        }
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
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }
}
