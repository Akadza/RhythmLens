package com.rimuru.android.rhythmlens.ui.app.features.comparison

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rimuru.android.rhythmlens.domain.model.EcgLead
import com.rimuru.android.rhythmlens.domain.model.EcgLeadOrigin
import com.rimuru.android.rhythmlens.domain.model.EcgLeadSegment
import com.rimuru.android.rhythmlens.domain.model.EcgPoint
import com.rimuru.android.rhythmlens.domain.model.EcgPrediction
import com.rimuru.android.rhythmlens.domain.model.EcgRecord
import com.rimuru.android.rhythmlens.domain.model.EcgStatus
import com.rimuru.android.rhythmlens.domain.usecase.GetEcgByIdUseCase
import com.rimuru.android.rhythmlens.domain.usecase.GetEcgListUseCase
import com.rimuru.android.rhythmlens.ui.navigation.ComparisonDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
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
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ComparisonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getEcgByIdUseCase: GetEcgByIdUseCase,
    private val getEcgListUseCase: GetEcgListUseCase
) : ViewModel() {

    private val destination = savedStateHandle.toRoute<ComparisonDestination>()
    private val selectedComparedId = MutableStateFlow(destination.comparedEcgId)

    private val _uiState = MutableStateFlow(
        ComparisonUiState(
            baseEcgId = destination.baseEcgId,
            isLoading = true
        )
    )
    val uiState = _uiState

    private val _effect = Channel<ComparisonEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        observeComparison()
    }

    fun onEvent(event: ComparisonEvent) {
        when (event) {
            ComparisonEvent.BackClicked -> {
                _effect.trySend(ComparisonEffect.NavigateBack)
            }

            is ComparisonEvent.ComparedRecordSelected -> {
                selectedComparedId.value = event.ecgId
            }

            is ComparisonEvent.SignalModeChanged -> {
                _uiState.update { state ->
                    state.copy(signalMode = event.mode)
                }
            }
        }
    }

    private fun observeComparison() {
        viewModelScope.launch {
            getEcgByIdUseCase(destination.baseEcgId)
                .flatMapLatest { baseRecord ->
                    if (baseRecord == null) {
                        flowOf(ComparisonData(baseRecord = null))
                    } else {
                        combine(
                            getEcgListUseCase(baseRecord.patientId),
                            selectedComparedId
                        ) { records, selectedId ->
                            val candidates = records
                                .filter { record ->
                                    record.id != baseRecord.id &&
                                        record.status == EcgStatus.PROCESSED &&
                                        record.digitizedSignal != null
                                }
                                .sortedByDescending { record -> record.recordedAt }
                            val resolvedSelectedId = selectedId
                                ?.takeIf { id -> candidates.any { candidate -> candidate.id == id } }
                                ?: candidates.firstOrNull()?.id
                            BaseAndCandidates(
                                baseRecord = baseRecord,
                                candidates = candidates,
                                selectedComparedId = resolvedSelectedId
                            )
                        }.flatMapLatest { data ->
                            val comparedFlow = data.selectedComparedId?.let { id ->
                                getEcgByIdUseCase(id)
                            } ?: flowOf(null)

                            comparedFlow.map { comparedRecord ->
                                ComparisonData(
                                    baseRecord = data.baseRecord,
                                    candidates = data.candidates,
                                    selectedComparedId = data.selectedComparedId,
                                    comparedRecord = comparedRecord
                                )
                            }
                        }
                    }
                }
                .catch { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Не удалось загрузить сравнение ЭКГ"
                        )
                    }
                }
                .collect { data ->
                    _uiState.update { state ->
                        data.toUiState(previousMode = state.signalMode)
                    }
                }
        }
    }

    private fun ComparisonData.toUiState(previousMode: ComparisonSignalModeUi): ComparisonUiState {
        val base = baseRecord
        if (base == null) {
            return ComparisonUiState(
                baseEcgId = destination.baseEcgId,
                isLoading = false,
                errorMessage = "Базовая запись ЭКГ не найдена"
            )
        }

        val compared = comparedRecord
        val leads = if (compared != null) {
            buildComparisonLeads(base, compared)
        } else {
            emptyList()
        }

        return ComparisonUiState(
            baseEcgId = base.id,
            baseTitle = base.title(),
            selectedComparedEcgId = selectedComparedId,
            candidates = candidates.map { record -> record.toCandidateUi() },
            leads = leads,
            summary = buildSummary(base, compared, leads),
            signalMode = previousMode,
            isLoading = false,
            errorMessage = null
        )
    }

    private fun EcgRecord.toCandidateUi(): ComparisonCandidateUi {
        return ComparisonCandidateUi(
            id = id,
            title = title(),
            subtitle = primaryPrediction?.label ?: "Результат анализа"
        )
    }

    private fun EcgRecord.title(): String {
        return DATE_FORMATTER.format(recordedAt.atZone(ZoneId.systemDefault()))
    }

    private fun buildComparisonLeads(base: EcgRecord, compared: EcgRecord): List<ComparisonLeadUi> {
        val baseSignal = base.digitizedSignal
        val comparedSignal = compared.digitizedSignal

        return EcgLead.entries.map { lead ->
            val basePoints = baseSignal?.leads?.get(lead).orEmpty()
            val comparedPoints = comparedSignal?.leads?.get(lead).orEmpty()
            ComparisonLeadUi(
                name = lead.name,
                basePoints = basePoints,
                comparedPoints = comparedPoints,
                baseSegments = baseSignal?.leadSegments?.get(lead)
                    ?: basePoints.toSingleSegment(baseSignal?.leadOrigins?.get(lead) ?: EcgLeadOrigin.DIGITIZED),
                comparedSegments = comparedSignal?.leadSegments?.get(lead)
                    ?: comparedPoints.toSingleSegment(comparedSignal?.leadOrigins?.get(lead) ?: EcgLeadOrigin.DIGITIZED)
            )
        }
    }

    private fun List<EcgPoint>.toSingleSegment(origin: EcgLeadOrigin): List<EcgLeadSegment> {
        if (isEmpty()) {
            return emptyList()
        }
        return listOf(
            EcgLeadSegment(
                origin = origin,
                startSampleIndex = 0,
                points = this
            )
        )
    }

    private fun buildSummary(
        base: EcgRecord,
        compared: EcgRecord?,
        leads: List<ComparisonLeadUi>
    ): ComparisonSummaryUi {
        if (compared == null) {
            return ComparisonSummaryUi(
                title = "Выберите запись для сравнения",
                points = listOf("Для сравнения нужна вторая обработанная ЭКГ этого же пациента.")
            )
        }

        val leadMetrics = leads.mapNotNull { lead ->
            val metric = comparePoints(lead.basePoints, lead.comparedPoints) ?: return@mapNotNull null
            lead.name to metric
        }
        val averageDifference = leadMetrics.map { (_, metric) -> metric.meanAbsDiffMv }.averageOrNull()
        val maxDifferenceLead = leadMetrics.maxByOrNull { (_, metric) -> metric.meanAbsDiffMv }
        val basePrediction = base.primaryPrediction.toReadablePrediction()
        val comparedPrediction = compared.primaryPrediction.toReadablePrediction()
        val predictionText = if (basePrediction == comparedPrediction) {
            "Основной класс анализа не изменился: $basePrediction."
        } else {
            "Основной класс анализа изменился: $basePrediction → $comparedPrediction."
        }

        val points = buildList {
            averageDifference?.let { value ->
                add("Среднее абсолютное расхождение по общим отведениям: ${value.formatMv()} мВ.")
            }
            maxDifferenceLead?.let { (leadName, metric) ->
                add("Наибольшее расхождение отмечено в отведении $leadName: ${metric.meanAbsDiffMv.formatMv()} мВ, коэффициент сходства ${metric.correlation.formatCorrelation()}.")
            }
            add(predictionText)
            add("Автоматическое сравнение является ориентировочным и не заменяет врачебную оценку формы комплексов, интервалов и сегмента ST.")
        }

        return ComparisonSummaryUi(
            title = "Краткое сравнение",
            points = points
        )
    }

    private fun comparePoints(
        first: List<EcgPoint>,
        second: List<EcgPoint>
    ): LeadDifferenceMetric? {
        val count = min(first.size, second.size)
        if (count < MIN_POINTS_FOR_COMPARISON) {
            return null
        }

        val stride = max(1, count / MAX_COMPARISON_POINTS)
        var samples = 0
        var absSum = 0.0
        var firstSum = 0.0
        var secondSum = 0.0
        var firstSquaredSum = 0.0
        var secondSquaredSum = 0.0
        var productSum = 0.0

        var index = 0
        while (index < count) {
            val firstValue = first[index].voltageMv
            val secondValue = second[index].voltageMv
            absSum += abs(firstValue - secondValue)
            firstSum += firstValue
            secondSum += secondValue
            firstSquaredSum += firstValue * firstValue
            secondSquaredSum += secondValue * secondValue
            productSum += firstValue * secondValue
            samples++
            index += stride
        }

        if (samples == 0) {
            return null
        }

        val numerator = productSum - (firstSum * secondSum / samples)
        val firstVariance = firstSquaredSum - (firstSum * firstSum / samples)
        val secondVariance = secondSquaredSum - (secondSum * secondSum / samples)
        val denominator = sqrt(max(0.0, firstVariance) * max(0.0, secondVariance))
        val correlation = if (denominator > 0.0) numerator / denominator else 0.0

        return LeadDifferenceMetric(
            meanAbsDiffMv = absSum / samples,
            correlation = correlation.coerceIn(-1.0, 1.0)
        )
    }

    private fun EcgPrediction?.toReadablePrediction(): String {
        return this?.label ?: "нет данных"
    }

    private fun List<Double>.averageOrNull(): Double? {
        return if (isEmpty()) null else average()
    }

    private fun Double.formatMv(): String {
        return String.format(Locale.getDefault(), "%.2f", this)
    }

    private fun Double.formatCorrelation(): String {
        return String.format(Locale.getDefault(), "%.2f", this)
    }

    private data class BaseAndCandidates(
        val baseRecord: EcgRecord,
        val candidates: List<EcgRecord>,
        val selectedComparedId: String?
    )

    private data class ComparisonData(
        val baseRecord: EcgRecord?,
        val candidates: List<EcgRecord> = emptyList(),
        val selectedComparedId: String? = null,
        val comparedRecord: EcgRecord? = null
    )

    private data class LeadDifferenceMetric(
        val meanAbsDiffMv: Double,
        val correlation: Double
    )

    private companion object {
        const val MIN_POINTS_FOR_COMPARISON = 20
        const val MAX_COMPARISON_POINTS = 1000
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }
}
