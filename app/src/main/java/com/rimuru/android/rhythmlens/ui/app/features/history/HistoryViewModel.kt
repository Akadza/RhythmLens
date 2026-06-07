package com.rimuru.android.rhythmlens.ui.app.features.history

import kotlin.math.roundToInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimuru.android.rhythmlens.domain.model.EcgLeadOrigin
import com.rimuru.android.rhythmlens.domain.model.EcgRecord
import com.rimuru.android.rhythmlens.domain.model.EcgStatus
import com.rimuru.android.rhythmlens.domain.usecase.GetEcgListUseCase
import com.rimuru.android.rhythmlens.domain.usecase.ObserveDoctorConclusionUseCase
import com.rimuru.android.rhythmlens.domain.usecase.ObservePatientByIdUseCase
import com.rimuru.android.rhythmlens.domain.usecase.ObserveSelectedPatientIdUseCase
import com.rimuru.android.rhythmlens.domain.usecase.RefreshEcgListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getEcgListUseCase: GetEcgListUseCase,
    private val refreshEcgListUseCase: RefreshEcgListUseCase,
    private val observeSelectedPatientIdUseCase: ObserveSelectedPatientIdUseCase,
    private val observePatientByIdUseCase: ObservePatientByIdUseCase,
    private val observeDoctorConclusionUseCase: ObserveDoctorConclusionUseCase
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

            val refreshError = runCatching {
                refreshEcgListUseCase()
            }.exceptionOrNull()

            observeSelectedPatientIdUseCase()
                .flatMapLatest { patientId ->
                    patientId?.let { id ->
                        combine(
                            getEcgListUseCase(id),
                            observePatientByIdUseCase(id)
                        ) { records, patient ->
                            records to patient?.fullName
                        }.flatMapLatest { (records, patientName) ->
                            observeConclusionFlags(records).map { conclusionFlags ->
                                HistoryData(
                                    records = records,
                                    patientName = patientName,
                                    conclusionFlags = conclusionFlags
                                )
                            }
                        }
                    } ?: flowOf(
                        HistoryData(
                            records = emptyList(),
                            patientName = null,
                            conclusionFlags = emptyMap()
                        )
                    )
                }
                .catch { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Не удалось загрузить историю ЭКГ"
                        )
                    }
                }
                .collect { data ->
                    _uiState.update {
                        val syncWarning = refreshError?.message
                            ?.takeIf { data.records.isEmpty() }
                            ?: refreshError?.let { "Не удалось обновить историю с сервера. Показаны локальные данные." }

                        HistoryUiState(
                            isLoading = false,
                            items = data.records.map { record ->
                                record.toUi(
                                    patientName = data.patientName ?: record.patientId,
                                    hasDoctorConclusion = data.conclusionFlags[record.id] == true
                                )
                            },
                            errorMessage = syncWarning
                        )
                    }
                }
        }
    }

    private fun observeConclusionFlags(records: List<EcgRecord>): Flow<Map<String, Boolean>> {
        if (records.isEmpty()) {
            return flowOf(emptyMap())
        }

        return combine(
            records.map { record ->
                observeDoctorConclusionUseCase(record.id).map { conclusion ->
                    record.id to (conclusion != null)
                }
            }
        ) { pairs ->
            pairs.toMap()
        }
    }

    private fun EcgRecord.toUi(
        patientName: String,
        hasDoctorConclusion: Boolean
    ): EcgHistoryItemUi {
        val statusText = processingMessage ?: status.toDisplayText()
        val prediction = primaryPrediction
        val digitizedLeads = digitizedSignal?.leadOrigins
            ?.count { (_, origin) -> origin == EcgLeadOrigin.DIGITIZED || origin == EcgLeadOrigin.MIXED }
            ?: 0
        val reconstructedLeads = digitizedSignal?.leadOrigins
            ?.count { (_, origin) -> origin == EcgLeadOrigin.RECONSTRUCTED || origin == EcgLeadOrigin.MIXED }
            ?: 0

        return EcgHistoryItemUi(
            id = id,
            date = DATE_FORMATTER.format(recordedAt.atZone(ZoneId.systemDefault())),
            patientName = patientName,
            mainResult = when {
                status == EcgStatus.ERROR -> errorMessage ?: statusText
                status == EcgStatus.PROCESSED && prediction != null -> prediction.label
                else -> statusText
            },
            probability = prediction?.probability?.toPercentInt() ?: 0,
            digitizedLeads = digitizedLeads,
            reconstructedLeads = reconstructedLeads,
            status = status.toUiStatus(),
            hasDoctorConclusion = hasDoctorConclusion
        )
    }

    private fun Double.toPercentInt(): Int {
        val percent = if (this <= 1.0) this * 100.0 else this
        return percent.roundToInt().coerceIn(0, 100)
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
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }
}

private data class HistoryData(
    val records: List<EcgRecord>,
    val patientName: String?,
    val conclusionFlags: Map<String, Boolean>
)
