package com.rimuru.android.rhythmlens.ui.app.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimuru.android.rhythmlens.domain.model.DigitizedEcg
import com.rimuru.android.rhythmlens.domain.model.EcgLead
import com.rimuru.android.rhythmlens.domain.model.EcgLeadOrigin
import com.rimuru.android.rhythmlens.domain.model.EcgPoint
import com.rimuru.android.rhythmlens.domain.model.EcgRecord
import com.rimuru.android.rhythmlens.domain.model.EcgStatus
import com.rimuru.android.rhythmlens.domain.usecase.GetEcgListUseCase
import com.rimuru.android.rhythmlens.domain.usecase.ObserveCurrentUserUseCase
import com.rimuru.android.rhythmlens.domain.usecase.ObservePatientByIdUseCase
import com.rimuru.android.rhythmlens.domain.usecase.ObserveSelectedPatientIdUseCase
import com.rimuru.android.rhythmlens.domain.usecase.SaveEcgUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    private val saveEcgUseCase: SaveEcgUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val observeSelectedPatientIdUseCase: ObserveSelectedPatientIdUseCase,
    private val observePatientByIdUseCase: ObservePatientByIdUseCase
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
            combine(
                observeCurrentUserUseCase(),
                observeSelectedPatientIdUseCase()
            ) { user, selectedPatientId ->
                user to selectedPatientId
            }
                .flatMapLatest { (user, selectedPatientId) ->
                    val patientNameFlow = selectedPatientId?.let { patientId ->
                        observePatientByIdUseCase(patientId).map { patient ->
                            patient?.fullName
                        }
                    } ?: flowOf(null)

                    patientNameFlow.flatMapLatest { selectedPatientName ->
                        _uiState.update { state ->
                            state.copy(
                                userName = user?.fullName.orEmpty(),
                                selectedPatientId = selectedPatientId,
                                selectedPatientName = selectedPatientName,
                                totalRecords = 0,
                                lastRecord = null
                            )
                        }

                        selectedPatientId?.let { patientId ->
                            getEcgListUseCase(patientId)
                        } ?: flowOf(emptyList())
                    }
                }
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
        val patientId = _uiState.value.selectedPatientId ?: return

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isCreatingTestEcg = true)
            }

            runCatching {
                val record = buildInitialTestEcgRecord(patientId = patientId)

                saveEcgUseCase(record.copy(status = EcgStatus.UPLOADING))
                delay(PROCESSING_STEP_DELAY_MS)

                saveEcgUseCase(record.copy(status = EcgStatus.DIGITIZING))
                delay(PROCESSING_STEP_DELAY_MS)

                saveEcgUseCase(record.copy(status = EcgStatus.COMPLETING))
                delay(PROCESSING_STEP_DELAY_MS)

                saveEcgUseCase(record.copy(status = EcgStatus.ANALYZING))
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

                val failedRecord = buildInitialTestEcgRecord(patientId = patientId).copy(
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
        val digitizedLeads = digitizedSignal?.leadOrigins
            ?.count { (_, origin) -> origin == EcgLeadOrigin.DIGITIZED || origin == EcgLeadOrigin.MIXED }
            ?: 0
        val reconstructedLeads = digitizedSignal?.leadOrigins
            ?.count { (_, origin) -> origin == EcgLeadOrigin.RECONSTRUCTED || origin == EcgLeadOrigin.MIXED }
            ?: 0
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

    private fun buildInitialTestEcgRecord(patientId: String): EcgRecord {
        return EcgRecord(
            id = UUID.randomUUID().toString(),
            patientId = patientId,
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
        val leadOrigins = EcgLead.entries.associateWith { lead ->
            if (lead in RECONSTRUCTED_TEST_LEADS) {
                EcgLeadOrigin.RECONSTRUCTED
            } else {
                EcgLeadOrigin.DIGITIZED
            }
        }

        return DigitizedEcg(
            leads = leads,
            leadOrigins = leadOrigins,
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
        const val SAMPLING_RATE = 500
        const val DURATION_SECONDS = 10.0
        const val HEART_RATE_HZ = 1.2
        const val PROCESSING_STEP_DELAY_MS = 700L
        val RECONSTRUCTED_TEST_LEADS = setOf(EcgLead.V3, EcgLead.V4, EcgLead.V5, EcgLead.V6)
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }
}

private fun initialState(): HomeUiState {
    return HomeUiState(
        userName = "",
        selectedPatientId = null,
        selectedPatientName = null,
        totalRecords = 0,
        linkedDoctorCount = 0,
        lastRecord = null
    )
}
