package com.rimuru.android.rhythmlens.ui.app.features.ecgdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rimuru.android.rhythmlens.domain.model.EcgLead
import com.rimuru.android.rhythmlens.domain.model.EcgLeadOrigin
import com.rimuru.android.rhythmlens.domain.model.EcgPoint
import com.rimuru.android.rhythmlens.domain.model.EcgRecord
import com.rimuru.android.rhythmlens.domain.model.EcgStatus
import com.rimuru.android.rhythmlens.domain.usecase.DeleteEcgUseCase
import com.rimuru.android.rhythmlens.domain.usecase.GetEcgByIdUseCase
import com.rimuru.android.rhythmlens.ui.app.features.ecgdetail.components.DiagnosisProbabilityUi
import com.rimuru.android.rhythmlens.ui.navigation.EcgDetailDestination
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
import kotlin.math.PI
import kotlin.math.sin

@HiltViewModel
class EcgDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getEcgByIdUseCase: GetEcgByIdUseCase,
    private val deleteEcgUseCase: DeleteEcgUseCase
) : ViewModel() {

    private val destination = savedStateHandle.toRoute<EcgDetailDestination>()
    val ecgId: String = destination.ecgId

    private val _uiState = MutableStateFlow(sampleInitialState(ecgId).copy(isLoading = true))
    val uiState = _uiState

    private val _effect = Channel<EcgDetailEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadEcg()
    }

    fun onEvent(event: EcgDetailEvent) {
        when (event) {
            EcgDetailEvent.BackClicked -> {
                _effect.trySend(EcgDetailEffect.NavigateBack)
            }

            EcgDetailEvent.CompareClicked -> {
                _effect.trySend(EcgDetailEffect.NavigateToComparison(ecgId))
            }

            EcgDetailEvent.SyntheticClicked -> {
                _effect.trySend(EcgDetailEffect.NavigateToSyntheticImage(ecgId))
            }

            EcgDetailEvent.ExportClicked -> {
                _effect.trySend(EcgDetailEffect.NavigateToExport(ecgId))
            }

            EcgDetailEvent.DoctorConclusionClicked -> {
                _effect.trySend(EcgDetailEffect.OpenDoctorConclusion(ecgId))
            }

            EcgDetailEvent.DeleteClicked -> {
                _uiState.update { state ->
                    state.copy(isDeleteDialogVisible = true)
                }
            }

            EcgDetailEvent.DeleteDismissed -> {
                _uiState.update { state ->
                    state.copy(isDeleteDialogVisible = false)
                }
            }

            EcgDetailEvent.DeleteConfirmed -> {
                deleteCurrentEcg()
            }

            is EcgDetailEvent.SignalModeChanged -> {
                _uiState.update { state ->
                    state.copy(signalMode = event.mode)
                }
            }
        }
    }

    private fun loadEcg() {
        viewModelScope.launch {
            getEcgByIdUseCase(ecgId)
                .catch { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Не удалось загрузить запись ЭКГ"
                        )
                    }
                }
                .collect { record ->
                    _uiState.update { currentState ->
                        record?.toUiState(previousState = currentState)
                            ?: currentState.copy(
                                isLoading = false,
                                errorMessage = "Запись ЭКГ не найдена"
                            )
                    }
                }
        }
    }

    private fun deleteCurrentEcg() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isDeleting = true)
            }

            runCatching {
                deleteEcgUseCase(ecgId)
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        isDeleting = false,
                        isDeleteDialogVisible = false
                    )
                }
                _effect.trySend(EcgDetailEffect.NavigateBack)
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isDeleting = false,
                        isDeleteDialogVisible = false,
                        errorMessage = throwable.message ?: "Не удалось удалить запись ЭКГ"
                    )
                }
            }
        }
    }

    private fun EcgRecord.toUiState(previousState: EcgDetailUiState): EcgDetailUiState {
        val fallback = sampleInitialState(id)
        val leads = buildLeadSummaryList(this)
        val digitizedLeads = leads.count { lead ->
            lead.origin == LeadOriginUi.Digitized || lead.origin == LeadOriginUi.Mixed
        }
        val reconstructedLeads = leads.count { lead ->
            lead.origin == LeadOriginUi.Reconstructed || lead.origin == LeadOriginUi.Mixed
        }
        val duration = digitizedSignal?.durationSeconds?.let { durationSeconds ->
            "$durationSeconds с"
        } ?: fallback.signalInfo.duration
        val samplingRate = digitizedSignal?.samplingRate?.let { rate ->
            "$rate Гц"
        } ?: fallback.signalInfo.samplingRate

        return fallback.copy(
            ecgId = id,
            date = DATE_FORMATTER.format(recordedAt.atZone(ZoneId.systemDefault())),
            signalInfo = fallback.signalInfo.copy(
                duration = duration,
                samplingRate = samplingRate,
                digitizedLeads = digitizedLeads,
                reconstructedLeads = reconstructedLeads
            ),
            leads = leads,
            isLoading = false,
            errorMessage = null,
            signalMode = previousState.signalMode,
            isDeleteDialogVisible = previousState.isDeleteDialogVisible,
            isDeleting = previousState.isDeleting
        )
    }

    private fun buildLeadSummaryList(record: EcgRecord): List<LeadSummaryUi> {
        val signal = record.digitizedSignal
        val shouldUseFallbackSignal = signal == null && record.status == EcgStatus.PROCESSED

        return EcgLead.entries.map { lead ->
            val origin = signal?.leadOrigins?.get(lead)?.toUiOrigin()
                ?: fallbackOriginForLead(lead)
            val points = signal?.leads?.get(lead)
                ?: if (shouldUseFallbackSignal) buildFallbackLeadPoints(lead) else emptyList()

            LeadSummaryUi(
                name = lead.name,
                origin = origin,
                points = points
            )
        }
    }

    private fun buildFallbackLeadPoints(lead: EcgLead): List<EcgPoint> {
        val phaseShift = lead.ordinal * 0.12
        val amplitude = 0.7 + lead.ordinal * 0.03
        val pointCount = (FALLBACK_SAMPLING_RATE * FALLBACK_DURATION_SECONDS).toInt()

        return List(pointCount) { index ->
            val timeMs = index * 1000L / FALLBACK_SAMPLING_RATE
            val timeSeconds = index.toDouble() / FALLBACK_SAMPLING_RATE
            val voltage = amplitude * sin(2.0 * PI * FALLBACK_HEART_RATE_HZ * timeSeconds + phaseShift)

            EcgPoint(
                timeMs = timeMs,
                voltageMv = voltage
            )
        }
    }

    private fun EcgLeadOrigin.toUiOrigin(): LeadOriginUi {
        return when (this) {
            EcgLeadOrigin.DIGITIZED -> LeadOriginUi.Digitized
            EcgLeadOrigin.RECONSTRUCTED -> LeadOriginUi.Reconstructed
            EcgLeadOrigin.MIXED -> LeadOriginUi.Mixed
        }
    }

    private fun fallbackOriginForLead(lead: EcgLead): LeadOriginUi {
        return when (lead) {
            EcgLead.V3,
            EcgLead.V4,
            EcgLead.V5,
            EcgLead.V6 -> LeadOriginUi.Reconstructed

            else -> LeadOriginUi.Digitized
        }
    }

    private companion object {
        const val FALLBACK_SAMPLING_RATE = 500
        const val FALLBACK_DURATION_SECONDS = 10.0
        const val FALLBACK_HEART_RATE_HZ = 1.2
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }
}

private fun sampleInitialState(ecgId: String): EcgDetailUiState {
    return EcgDetailUiState(
        ecgId = ecgId,
        date = "28.05.2026",
        probabilities = listOf(
            DiagnosisProbabilityUi(
                title = "Фибрилляция предсердий",
                code = "AF",
                probability = 50
            ),
            DiagnosisProbabilityUi(
                title = "ST-изменения",
                code = null,
                probability = 20
            ),
            DiagnosisProbabilityUi(
                title = "Блокада правой ножки пучка Гиса",
                code = null,
                probability = 10
            ),
            DiagnosisProbabilityUi(
                title = "Норма",
                code = null,
                probability = 5
            )
        ),
        signalInfo = SignalInfoUi(
            duration = "10 с",
            samplingRate = "500 Гц",
            digitizedLeads = 8,
            reconstructedLeads = 4,
            source = "Фото",
            quality = "Среднее"
        ),
        leads = listOf(
            LeadSummaryUi("I", LeadOriginUi.Digitized),
            LeadSummaryUi("II", LeadOriginUi.Digitized),
            LeadSummaryUi("III", LeadOriginUi.Digitized),
            LeadSummaryUi("aVR", LeadOriginUi.Digitized),
            LeadSummaryUi("aVL", LeadOriginUi.Digitized),
            LeadSummaryUi("aVF", LeadOriginUi.Digitized),
            LeadSummaryUi("V1", LeadOriginUi.Digitized),
            LeadSummaryUi("V2", LeadOriginUi.Digitized),
            LeadSummaryUi("V3", LeadOriginUi.Reconstructed),
            LeadSummaryUi("V4", LeadOriginUi.Reconstructed),
            LeadSummaryUi("V5", LeadOriginUi.Reconstructed),
            LeadSummaryUi("V6", LeadOriginUi.Reconstructed)
        )
    )
}
