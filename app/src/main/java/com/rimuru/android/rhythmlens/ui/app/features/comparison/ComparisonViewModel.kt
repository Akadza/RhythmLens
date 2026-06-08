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
import com.rimuru.android.rhythmlens.domain.usecase.GetCachedEcgListUseCase
import com.rimuru.android.rhythmlens.domain.usecase.GetEcgByIdUseCase
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
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ComparisonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getEcgByIdUseCase: GetEcgByIdUseCase,
    private val getCachedEcgListUseCase: GetCachedEcgListUseCase
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
                            getCachedEcgListUseCase(baseRecord.patientId),
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
            val comparedRawPoints = comparedSignal?.leads?.get(lead).orEmpty()
            val baseSegments = baseSignal?.leadSegments?.get(lead)
                ?: basePoints.toSingleSegment(baseSignal?.leadOrigins?.get(lead) ?: EcgLeadOrigin.DIGITIZED)
            val comparedRawSegments = comparedSignal?.leadSegments?.get(lead)
                ?: comparedRawPoints.toSingleSegment(comparedSignal?.leadOrigins?.get(lead) ?: EcgLeadOrigin.DIGITIZED)
            val shiftMs = estimateShiftMs(basePoints, comparedRawPoints) ?: 0L
            val minTimeMs = basePoints.minOfOrNull { point -> point.timeMs } ?: 0L
            val maxTimeMs = basePoints.maxOfOrNull { point -> point.timeMs } ?: Long.MAX_VALUE
            val comparedPoints = comparedRawPoints.shiftPointsAndCrop(
                shiftMs = shiftMs,
                minTimeMs = minTimeMs,
                maxTimeMs = maxTimeMs
            )
            val comparedSegments = comparedRawSegments.shiftSegmentsAndCrop(
                shiftMs = shiftMs,
                minTimeMs = minTimeMs,
                maxTimeMs = maxTimeMs
            )

            ComparisonLeadUi(
                name = lead.name,
                basePoints = basePoints,
                comparedPoints = comparedPoints,
                baseSegments = baseSegments,
                comparedSegments = comparedSegments
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
        val maxDifferenceLead = leadMetrics.maxByOrNull { (_, metric) -> metric.meanAbsDiffMv }
        val basePrediction = base.primaryPrediction.toReadablePrediction()
        val comparedPrediction = compared.primaryPrediction.toReadablePrediction()
        val predictionText = if (basePrediction == comparedPrediction) {
            "Основной класс анализа не изменился: $basePrediction."
        } else {
            "Основной класс анализа изменился: $basePrediction → $comparedPrediction."
        }

        val points = buildList {
            add("Перед наложением каждое отведение сравниваемой ЭКГ автоматически выровнено по форме первого участка сигнала.")
            maxDifferenceLead?.let { (leadName, _) ->
                add("Наибольшее расхождение формы сигнала отмечено в отведении $leadName.")
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
        if (first.size < MIN_POINTS_FOR_COMPARISON || second.size < MIN_POINTS_FOR_COMPARISON) {
            return null
        }

        val firstSorted = first.sortedBy { point -> point.timeMs }
        val secondSorted = second.sortedBy { point -> point.timeMs }
        val stride = max(1, firstSorted.size / MAX_COMPARISON_POINTS)
        var secondIndex = 0
        var samples = 0
        var absSum = 0.0
        var firstSum = 0.0
        var secondSum = 0.0
        var firstSquaredSum = 0.0
        var secondSquaredSum = 0.0
        var productSum = 0.0

        var firstIndex = 0
        while (firstIndex < firstSorted.size) {
            val firstPoint = firstSorted[firstIndex]
            while (
                secondIndex + 1 < secondSorted.size &&
                abs(secondSorted[secondIndex + 1].timeMs - firstPoint.timeMs) <= abs(secondSorted[secondIndex].timeMs - firstPoint.timeMs)
            ) {
                secondIndex++
            }

            val secondPoint = secondSorted[secondIndex]
            if (abs(secondPoint.timeMs - firstPoint.timeMs) <= MAX_PAIR_TIME_DIFF_MS) {
                val firstValue = firstPoint.voltageMv
                val secondValue = secondPoint.voltageMv
                absSum += abs(firstValue - secondValue)
                firstSum += firstValue
                secondSum += secondValue
                firstSquaredSum += firstValue * firstValue
                secondSquaredSum += secondValue * secondValue
                productSum += firstValue * secondValue
                samples++
            }
            firstIndex += stride
        }

        if (samples < MIN_POINTS_FOR_COMPARISON) {
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

    private fun estimateShiftMs(basePoints: List<EcgPoint>, comparedPoints: List<EcgPoint>): Long? {
        val baseSamples = buildAlignmentSamples(basePoints)
        val comparedSamples = buildAlignmentSamples(comparedPoints)
        if (baseSamples.size < MIN_ALIGNMENT_PAIRS || comparedSamples.size < MIN_ALIGNMENT_PAIRS) {
            return null
        }

        val coarseShifts = (buildCandidateShifts(baseSamples, comparedSamples) + buildCoarseShifts())
            .map { shift -> shift.coerceIn(-MAX_ALIGNMENT_SHIFT_MS, MAX_ALIGNMENT_SHIFT_MS) }
            .distinct()
        val coarseBest = coarseShifts
            .mapNotNull { shift -> scoreAlignmentShift(baseSamples, comparedSamples, shift) }
            .maxByOrNull { score -> score.score }
            ?: return null

        val refinedBest = buildRefinedShifts(coarseBest.shiftMs)
            .mapNotNull { shift -> scoreAlignmentShift(baseSamples, comparedSamples, shift) }
            .maxByOrNull { score -> score.score }
            ?: coarseBest

        return refinedBest
            .takeIf { score -> score.score >= MIN_ALIGNMENT_SCORE && score.pairs >= MIN_ALIGNMENT_PAIRS }
            ?.shiftMs
    }

    private fun buildAlignmentSamples(points: List<EcgPoint>): List<AlignmentSample> {
        val sortedPoints = points
            .asSequence()
            .filter { point -> point.timeMs in 0L..ALIGNMENT_WINDOW_MS }
            .sortedBy { point -> point.timeMs }
            .toList()
        if (sortedPoints.size < MIN_POINTS_FOR_COMPARISON) {
            return emptyList()
        }

        val rawValues = sortedPoints.map { point -> point.voltageMv }
        val baseline = median(rawValues)
        val centeredValues = rawValues.map { value -> value - baseline }
        val amplitudeScale = max(
            MIN_ALIGNMENT_SCALE_MV,
            percentile(centeredValues.map { value -> abs(value) }, 0.95)
        )
        val normalizedValues = centeredValues.map { value ->
            (value / amplitudeScale).coerceIn(-MAX_NORMALIZED_AMPLITUDE, MAX_NORMALIZED_AMPLITUDE)
        }
        val rawEnergy = normalizedValues.mapIndexed { index, value ->
            val slope = if (index == 0 || index == normalizedValues.lastIndex) {
                0.0
            } else {
                abs(normalizedValues[index + 1] - normalizedValues[index - 1]) / 2.0
            }
            abs(value) + slope * QRS_SLOPE_WEIGHT
        }
        val smoothedEnergy = rawEnergy.mapIndexed { index, value ->
            val previous = rawEnergy.getOrElse(index - 1) { value }
            val next = rawEnergy.getOrElse(index + 1) { value }
            (previous + value * 2.0 + next) / 4.0
        }

        return sortedPoints.mapIndexed { index, point ->
            AlignmentSample(
                timeMs = point.timeMs,
                value = normalizedValues[index],
                energy = smoothedEnergy[index]
            )
        }
    }

    private fun detectAlignmentPeaks(samples: List<AlignmentSample>): List<AlignmentPeak> {
        if (samples.size < MIN_POINTS_FOR_COMPARISON) {
            return emptyList()
        }

        val energyValues = samples.map { sample -> sample.energy }
        val threshold = max(MIN_ALIGNMENT_ENERGY, percentile(energyValues, 0.90) * PEAK_ENERGY_FRACTION)
        val potentialPeaks = samples.indices
            .drop(1)
            .dropLast(1)
            .mapNotNull { index ->
                val current = samples[index]
                if (
                    current.energy >= threshold &&
                    current.energy >= samples[index - 1].energy &&
                    current.energy >= samples[index + 1].energy
                ) {
                    AlignmentPeak(
                        timeMs = current.timeMs,
                        strength = current.energy
                    )
                } else {
                    null
                }
            }
            .sortedByDescending { peak -> peak.strength }

        val accepted = mutableListOf<AlignmentPeak>()
        potentialPeaks.forEach { peak ->
            val isFarEnough = accepted.none { acceptedPeak ->
                abs(acceptedPeak.timeMs - peak.timeMs) < MIN_QRS_DISTANCE_MS
            }
            if (isFarEnough) {
                accepted.add(peak)
            }
        }

        return accepted.sortedBy { peak -> peak.timeMs }
    }

    private fun buildCandidateShifts(
        baseSamples: List<AlignmentSample>,
        comparedSamples: List<AlignmentSample>
    ): List<Long> {
        val basePeaks = selectAlignmentPeaks(detectAlignmentPeaks(baseSamples))
        val comparedPeaks = selectAlignmentPeaks(detectAlignmentPeaks(comparedSamples))
        val shifts = mutableSetOf<Long>()
        basePeaks.forEach { basePeak ->
            comparedPeaks.forEach { comparedPeak ->
                val shift = basePeak.timeMs - comparedPeak.timeMs
                if (abs(shift) <= MAX_ALIGNMENT_SHIFT_MS) {
                    shifts.add(shift)
                }
            }
        }
        shifts.add(0L)
        return shifts.toList()
    }

    private fun selectAlignmentPeaks(peaks: List<AlignmentPeak>): List<AlignmentPeak> {
        return (peaks.take(MAX_TIME_ORDERED_PEAKS) + peaks.sortedByDescending { peak -> peak.strength }.take(MAX_STRONG_PEAKS))
            .distinctBy { peak -> peak.timeMs }
    }

    private fun buildCoarseShifts(): List<Long> {
        val shifts = mutableListOf<Long>()
        var shift = -MAX_ALIGNMENT_SHIFT_MS
        while (shift <= MAX_ALIGNMENT_SHIFT_MS) {
            shifts.add(shift)
            shift += COARSE_SHIFT_STEP_MS
        }
        return shifts
    }

    private fun buildRefinedShifts(centerShiftMs: Long): List<Long> {
        val shifts = mutableListOf<Long>()
        var shift = centerShiftMs - FINE_SHIFT_RADIUS_MS
        while (shift <= centerShiftMs + FINE_SHIFT_RADIUS_MS) {
            shifts.add(shift.coerceIn(-MAX_ALIGNMENT_SHIFT_MS, MAX_ALIGNMENT_SHIFT_MS))
            shift += FINE_SHIFT_STEP_MS
        }
        return shifts.distinct()
    }

    private fun scoreAlignmentShift(
        baseSamples: List<AlignmentSample>,
        comparedSamples: List<AlignmentSample>,
        shiftMs: Long
    ): AlignmentScore? {
        if (baseSamples.isEmpty() || comparedSamples.isEmpty()) {
            return null
        }

        val stride = max(1, baseSamples.size / MAX_ALIGNMENT_SAMPLES)
        var comparedIndex = 0
        var pairs = 0

        var baseValueSum = 0.0
        var comparedValueSum = 0.0
        var baseValueSquareSum = 0.0
        var comparedValueSquareSum = 0.0
        var valueProductSum = 0.0

        var baseEnergySum = 0.0
        var comparedEnergySum = 0.0
        var baseEnergySquareSum = 0.0
        var comparedEnergySquareSum = 0.0
        var energyProductSum = 0.0

        var baseIndex = 0
        while (baseIndex < baseSamples.size) {
            val baseSample = baseSamples[baseIndex]
            while (
                comparedIndex + 1 < comparedSamples.size &&
                abs((comparedSamples[comparedIndex + 1].timeMs + shiftMs) - baseSample.timeMs) <=
                abs((comparedSamples[comparedIndex].timeMs + shiftMs) - baseSample.timeMs)
            ) {
                comparedIndex++
            }

            val comparedSample = comparedSamples[comparedIndex]
            val shiftedComparedTime = comparedSample.timeMs + shiftMs
            if (abs(shiftedComparedTime - baseSample.timeMs) <= MAX_PAIR_TIME_DIFF_MS) {
                baseValueSum += baseSample.value
                comparedValueSum += comparedSample.value
                baseValueSquareSum += baseSample.value * baseSample.value
                comparedValueSquareSum += comparedSample.value * comparedSample.value
                valueProductSum += baseSample.value * comparedSample.value

                baseEnergySum += baseSample.energy
                comparedEnergySum += comparedSample.energy
                baseEnergySquareSum += baseSample.energy * baseSample.energy
                comparedEnergySquareSum += comparedSample.energy * comparedSample.energy
                energyProductSum += baseSample.energy * comparedSample.energy

                pairs++
            }

            baseIndex += stride
        }

        if (pairs < MIN_ALIGNMENT_PAIRS) {
            return null
        }

        val valueCorrelation = pearsonCorrelation(
            firstSum = baseValueSum,
            secondSum = comparedValueSum,
            firstSquareSum = baseValueSquareSum,
            secondSquareSum = comparedValueSquareSum,
            productSum = valueProductSum,
            count = pairs
        ).coerceAtLeast(0.0)
        val energyCorrelation = pearsonCorrelation(
            firstSum = baseEnergySum,
            secondSum = comparedEnergySum,
            firstSquareSum = baseEnergySquareSum,
            secondSquareSum = comparedEnergySquareSum,
            productSum = energyProductSum,
            count = pairs
        ).coerceAtLeast(0.0)
        val coverage = (pairs.toDouble() / TARGET_ALIGNMENT_PAIRS).coerceAtMost(1.0)
        val shiftPenalty = abs(shiftMs).toDouble() / MAX_ALIGNMENT_SHIFT_MS * SHIFT_PENALTY_WEIGHT
        val score = (valueCorrelation * VALUE_CORRELATION_WEIGHT + energyCorrelation * ENERGY_CORRELATION_WEIGHT) * coverage - shiftPenalty

        return AlignmentScore(
            shiftMs = shiftMs,
            score = score,
            pairs = pairs
        )
    }

    private fun pearsonCorrelation(
        firstSum: Double,
        secondSum: Double,
        firstSquareSum: Double,
        secondSquareSum: Double,
        productSum: Double,
        count: Int
    ): Double {
        val numerator = productSum - (firstSum * secondSum / count)
        val firstVariance = firstSquareSum - (firstSum * firstSum / count)
        val secondVariance = secondSquareSum - (secondSum * secondSum / count)
        val denominator = sqrt(max(0.0, firstVariance) * max(0.0, secondVariance))
        return if (denominator > 0.0) {
            (numerator / denominator).coerceIn(-1.0, 1.0)
        } else {
            0.0
        }
    }

    private fun EcgPrediction?.toReadablePrediction(): String {
        return this?.label ?: "нет данных"
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        return sorted[sorted.size / 2]
    }

    private fun percentile(values: List<Double>, fraction: Double): Double {
        if (values.isEmpty()) {
            return 0.0
        }
        val sorted = values.sorted()
        val index = ((sorted.size - 1) * fraction).toInt().coerceIn(0, sorted.lastIndex)
        return sorted[index]
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

    private data class AlignmentSample(
        val timeMs: Long,
        val value: Double,
        val energy: Double
    )

    private data class AlignmentPeak(
        val timeMs: Long,
        val strength: Double
    )

    private data class AlignmentScore(
        val shiftMs: Long,
        val score: Double,
        val pairs: Int
    )

    private companion object {
        const val MIN_POINTS_FOR_COMPARISON = 20
        const val MAX_COMPARISON_POINTS = 1000
        const val MAX_PAIR_TIME_DIFF_MS = 16L

        const val ALIGNMENT_WINDOW_MS = 3500L
        const val MAX_ALIGNMENT_SHIFT_MS = 700L
        const val COARSE_SHIFT_STEP_MS = 20L
        const val FINE_SHIFT_RADIUS_MS = 30L
        const val FINE_SHIFT_STEP_MS = 2L
        const val MAX_ALIGNMENT_SAMPLES = 700
        const val MIN_ALIGNMENT_PAIRS = 60
        const val TARGET_ALIGNMENT_PAIRS = 280
        const val MIN_ALIGNMENT_SCORE = 0.18

        const val MIN_QRS_DISTANCE_MS = 250L
        const val MIN_ALIGNMENT_SCALE_MV = 0.04
        const val MAX_NORMALIZED_AMPLITUDE = 6.0
        const val QRS_SLOPE_WEIGHT = 0.55
        const val MIN_ALIGNMENT_ENERGY = 0.60
        const val PEAK_ENERGY_FRACTION = 0.72
        const val MAX_TIME_ORDERED_PEAKS = 6
        const val MAX_STRONG_PEAKS = 6

        const val VALUE_CORRELATION_WEIGHT = 0.80
        const val ENERGY_CORRELATION_WEIGHT = 0.20
        const val SHIFT_PENALTY_WEIGHT = 0.03

        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }
}
