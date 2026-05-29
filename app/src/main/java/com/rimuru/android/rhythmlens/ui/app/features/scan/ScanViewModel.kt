package com.rimuru.android.rhythmlens.ui.app.features.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimuru.android.rhythmlens.domain.model.DigitizedEcg
import com.rimuru.android.rhythmlens.domain.model.EcgLead
import com.rimuru.android.rhythmlens.domain.model.EcgPoint
import com.rimuru.android.rhythmlens.domain.model.EcgRecord
import com.rimuru.android.rhythmlens.domain.model.EcgStatus
import com.rimuru.android.rhythmlens.domain.usecase.SaveEcgUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.sin

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val saveEcgUseCase: SaveEcgUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = Channel<ScanEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: ScanEvent) {
        when (event) {
            ScanEvent.BackClicked -> {
                _effect.trySend(ScanEffect.NavigateBack)
            }

            ScanEvent.TakePhotoClicked -> {
                _effect.trySend(ScanEffect.OpenCamera)
            }

            ScanEvent.ChooseFromGalleryClicked -> {
                _effect.trySend(ScanEffect.OpenGalleryPicker)
            }

            ScanEvent.CreateTestEcgClicked -> {
                createTestEcg()
            }
        }
    }

    private fun createTestEcg() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isProcessing = true, errorMessage = null)
            }

            runCatching {
                val record = buildInitialTestEcgRecord()

                saveEcgUseCase(
                    record.copy(
                        status = EcgStatus.UPLOADING,
                        processingMessage = "Загрузка изображения"
                    )
                )
                delay(PROCESSING_STEP_DELAY_MS)

                saveEcgUseCase(
                    record.copy(
                        status = EcgStatus.DIGITIZING,
                        processingMessage = "Оцифровка ЭКГ"
                    )
                )
                delay(PROCESSING_STEP_DELAY_MS)

                saveEcgUseCase(
                    record.copy(
                        status = EcgStatus.COMPLETING,
                        processingMessage = "Восстановление недостающих отведений"
                    )
                )
                delay(PROCESSING_STEP_DELAY_MS)

                saveEcgUseCase(
                    record.copy(
                        status = EcgStatus.ANALYZING,
                        processingMessage = "ИИ-анализ ЭКГ"
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
            }.onSuccess { record ->
                _uiState.update { state ->
                    state.copy(isProcessing = false)
                }
                _effect.trySend(ScanEffect.NavigateToEcgDetail(record.id))
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isProcessing = false,
                        errorMessage = throwable.message ?: "Не удалось создать тестовую ЭКГ"
                    )
                }
            }
        }
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

    private companion object {
        const val DEFAULT_PATIENT_ID = "temp-patient-id"
        const val SAMPLING_RATE = 500
        const val DURATION_SECONDS = 10.0
        const val HEART_RATE_HZ = 1.2
        const val PROCESSING_STEP_DELAY_MS = 700L
    }
}
