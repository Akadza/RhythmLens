package com.rimuru.android.rhythmlens.ui.app.features.ecgdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rimuru.android.rhythmlens.domain.model.EcgLead
import com.rimuru.android.rhythmlens.domain.model.EcgRecord
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

@HiltViewModel
class EcgDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getEcgByIdUseCase: GetEcgByIdUseCase
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
                _effect.trySend(EcgDetailEffect.ConfirmDelete(ecgId))
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

    private fun EcgRecord.toUiState(previousState: EcgDetailUiState): EcgDetailUiState {
        val fallback = sampleInitialState(id)
        val digitizedLeads = digitizedSignal?.leads?.size ?: fallback.signalInfo.digitizedLeads
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
                reconstructedLeads = STANDARD_LEAD_COUNT - digitizedLeads
            ),
            leads = buildLeadSummaryList(this),
            isLoading = false,
            errorMessage = null,
            signalMode = previousState.signalMode
        )
    }

    private fun buildLeadSummaryList(record: EcgRecord): List<LeadSummaryUi> {
        val availableLeads = record.digitizedSignal?.leads?.keys.orEmpty()

        return EcgLead.entries.map { lead ->
            LeadSummaryUi(
                name = lead.name,
                origin = when {
                    availableLeads.isEmpty() -> LeadOriginUi.Digitized
                    lead in availableLeads -> LeadOriginUi.Digitized
                    else -> LeadOriginUi.Reconstructed
                }
            )
        }
    }

    private companion object {
        const val STANDARD_LEAD_COUNT = 12
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
