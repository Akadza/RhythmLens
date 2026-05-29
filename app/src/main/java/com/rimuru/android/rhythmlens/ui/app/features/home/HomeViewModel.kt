package com.rimuru.android.rhythmlens.ui.app.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimuru.android.rhythmlens.domain.model.DigitizedEcg
import com.rimuru.android.rhythmlens.domain.model.EcgLead
import com.rimuru.android.rhythmlens.domain.model.EcgPoint
import com.rimuru.android.rhythmlens.domain.model.EcgRecord
import com.rimuru.android.rhythmlens.domain.model.EcgStatus
import com.rimuru.android.rhythmlens.domain.usecase.GetEcgListUseCase
import com.rimuru.android.rhythmlens.domain.usecase.SaveEcgUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.sin

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getEcgListUseCase: GetEcgListUseCase,
    private val saveEcgUseCase: SaveEcgUseCase
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
                _effect.trySend(HomeEffect.OpenCamera)
            }

            HomeEvent.GalleryClicked -> {
                closeSheet()
                _effect.trySend(HomeEffect.OpenGalleryPicker)
            }

            HomeEvent.ImportClicked -> {
                closeSheet()
                _effect.trySend(HomeEffect.OpenFilePicker)
            }

            HomeEvent.CreateTestEcgClicked -> {
                closeSheet()
                createTestEcg()
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

    private fun createTestEcg() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isCreatingTestEcg = true)
            }

            runCatching {
                val record = buildInitialTestEcgRecord()

                saveEcgUseCase(
                    record.copy(
                        status = EcgStatus.UPLOADING,
                        processingMessage = null
                    )
                )
                delay(PROCESSING_STEP_DELAY_MS)

                saveEcgUseCase(
                    record.copy(
                        status = EcgStatus.DIGITIZING,
                        processingMessage = null
                    )
                )
                delay(PROCESSING_STEP_DELAY_MS)

                saveEcgUseCase(
                    record.copy(
                        status = EcgStatus.COMPLETING,
                        processingMessage = null
                    )
                )
                delay(PROCESSING_STEP_DELAY_MS)

                saveEcgUseCase(
                    record.copy(
                        status = EcgStatus.ANALYZING,
                        processingMessage = null
                    )
                )
                delay(PROCESSING_STEP_DELAY_MS)

                val processedRecord = record.copy(
                    digitizedSignal = buildTestDigitizedEcg(),
                    heartRate = 72,
                    status = EcgStatus.PROCESSED,
                    processingMessage = null,
                    errorMessage = null
                )
                saveEcgUseCase(processedRecord)
                processedRecord
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(isCreatingTestEcg = false)
                }
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(isCreatingTestEcg = false)
                }

                val failedRecord = buildInitialTestEcgRecord().copy(
                    status = EcgStatus.ERROR,
                    errorMessage = throwable.message ?: "Не удалось создать тестовую ЭКГ"
                )
                saveEcgUseCase(failedRecord)
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
            probability = if (status == EcgStatus.PROCESSED) 0 else null,
            digitizedLeads = digitizedLeads,
            reconstructedLeads = reconstructedLeads
        )
    }

    private fun buildInitialTestEcgRecord(): EcgRecord {
        return EcgRecord(
            id = UUID.randomUUID().toString(),
            patientId = DEFAULT_PATIENT_ID,
            recordedAt = Instant.now(),
            originalImageUrl = null,
            digitizedSignal = null,
            heartRate = null,
            status = EcgStatus.DRAFT,
            processingMessage = null,
            errorMessage = null
        )
    }

    private fun buildTestDigitizedEcg(): DigitizedEcg {
        val leads = EcgLead.entries.associateWith { lead ->
            buildSyntheticLead(lead)
        }

        return DigitizedEcg(
            leads = leads,
            samplingRate = SAMPLING_RATE,
            durationSeconds = DURATION_SECONDS
        )
    }

    private fun buildSyntheticLead(lead: EcgLead): List<EcgPoint> {
        val phaseShift = lead.ordinal * 0.12
        val amplitude = 0.7 + lead.ordinal * 0.03
        val pointCount = (SAMPLING_RATE * DURATION_SECONDS).toInt()

        return List(pointCount) { index ->
            val timeMs = index * 1000L / SAMPLING_RATE
            val timeSeconds = index.toDouble() / SAMPLING_RATE
            val voltage = amplitude * sin(2.0 * PI * HEART_RATE_HZ * timeSeconds + phaseShift)

            EcgPoint(
                timeMs = timeMs,
                voltageMv = voltage
            )
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
        const val STANDARD_LEAD_COUNT = 12
        const val SAMPLING_RATE = 500
        const val DURATION_SECONDS = 10.0
        const val HEART_RATE_HZ = 1.2
        const val PROCESSING_STEP_DELAY_MS = 700L
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
