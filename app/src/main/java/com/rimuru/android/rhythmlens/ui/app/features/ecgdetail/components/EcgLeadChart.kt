package com.rimuru.android.rhythmlens.ui.app.features.ecgdetail.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.domain.model.EcgPoint
import com.rimuru.android.rhythmlens.ui.app.features.ecgdetail.LeadOriginUi
import com.rimuru.android.rhythmlens.ui.theme.RhythmSpacing
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

@Composable
fun EcgLeadChart(
    leadName: String,
    points: List<EcgPoint>,
    origin: LeadOriginUi,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val gridMinorColor = colorScheme.outlineVariant.copy(alpha = 0.30f)
    val gridMajorColor = colorScheme.outline.copy(alpha = 0.45f)
    val signalColor = when (origin) {
        LeadOriginUi.Digitized -> colorScheme.primary
        LeadOriginUi.Reconstructed -> colorScheme.tertiary
        LeadOriginUi.Mixed -> colorScheme.secondary
    }
    val axisColor = colorScheme.onSurfaceVariant.copy(alpha = 0.80f)
    val labelColor = colorScheme.onSurfaceVariant

    var zoomX by remember(points) { mutableStateOf(1f) }
    var offsetXFraction by remember(points) { mutableStateOf(0f) }

    Box(modifier = modifier) {
        if (points.isEmpty()) {
            Text(
                text = stringResource(R.string.lead_chart_no_data_template, leadName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(RhythmSpacing.Large)
            )
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points) {
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            val newZoom = (zoomX * gestureZoom).coerceIn(MIN_ZOOM_X, MAX_ZOOM_X)
                            zoomX = newZoom

                            val maxOffset = (1f - 1f / newZoom).coerceAtLeast(0f)
                            val panFraction = -pan.x / size.width.toFloat().coerceAtLeast(1f) / newZoom
                            offsetXFraction = (offsetXFraction + panFraction).coerceIn(0f, maxOffset)
                        }
                    }
            ) {
                val chartLeft = 44.dp.toPx()
                val chartRight = size.width - 12.dp.toPx()
                val chartTop = 12.dp.toPx()
                val chartBottom = size.height - 34.dp.toPx()
                val chartWidth = max(1f, chartRight - chartLeft)
                val chartHeight = max(1f, chartBottom - chartTop)
                val centerY = chartTop + chartHeight / 2f

                val fullTimeMinMs = points.first().timeMs.toFloat()
                val fullTimeMaxMs = points.last().timeMs.toFloat().coerceAtLeast(fullTimeMinMs + 1f)
                val fullTimeRangeMs = fullTimeMaxMs - fullTimeMinMs
                val visibleRangeMs = fullTimeRangeMs / zoomX
                val safeOffset = offsetXFraction.coerceIn(0f, (1f - 1f / zoomX).coerceAtLeast(0f))
                val visibleTimeMinMs = fullTimeMinMs + fullTimeRangeMs * safeOffset
                val visibleTimeMaxMs = visibleTimeMinMs + visibleRangeMs

                val visiblePoints = points.filter { point ->
                    point.timeMs >= visibleTimeMinMs && point.timeMs <= visibleTimeMaxMs
                }.ifEmpty { points }

                val voltageMaxAbs = max(
                    MIN_VOLTAGE_RANGE_MV,
                    visiblePoints.maxOf { abs(it.voltageMv) }
                ).toFloat()
                val voltageRange = voltageMaxAbs * 2f

                drawGrid(
                    chartLeft = chartLeft,
                    chartRight = chartRight,
                    chartTop = chartTop,
                    chartBottom = chartBottom,
                    minorColor = gridMinorColor,
                    majorColor = gridMajorColor
                )

                drawLine(
                    color = axisColor,
                    start = Offset(chartLeft, centerY),
                    end = Offset(chartRight, centerY),
                    strokeWidth = 1.dp.toPx()
                )

                drawYAxisLabels(
                    chartLeft = chartLeft,
                    chartTop = chartTop,
                    chartBottom = chartBottom,
                    voltageMaxAbs = voltageMaxAbs,
                    labelColor = labelColor
                )

                drawXAxisLabels(
                    chartLeft = chartLeft,
                    chartRight = chartRight,
                    chartBottom = chartBottom,
                    visibleTimeMinMs = visibleTimeMinMs,
                    visibleTimeMaxMs = visibleTimeMaxMs,
                    labelColor = labelColor
                )

                val path = Path()
                visiblePoints.forEachIndexed { index, point ->
                    val x = chartLeft + ((point.timeMs - visibleTimeMinMs) / visibleRangeMs) * chartWidth
                    val y = chartTop + ((voltageMaxAbs - point.voltageMv.toFloat()) / voltageRange) * chartHeight

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = signalColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }

            Text(
                text = stringResource(R.string.ecg_voltage_unit),
                style = MaterialTheme.typography.labelSmall,
                color = axisColor,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = RhythmSpacing.Small, top = RhythmSpacing.Small)
            )

            Text(
                text = stringResource(R.string.ecg_time_unit),
                style = MaterialTheme.typography.labelSmall,
                color = axisColor,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = RhythmSpacing.Small, bottom = RhythmSpacing.ExtraSmall)
            )

            Text(
                text = stringResource(R.string.ecg_chart_scale),
                style = MaterialTheme.typography.labelSmall,
                color = axisColor,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = RhythmSpacing.Small, bottom = RhythmSpacing.ExtraSmall)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid(
    chartLeft: Float,
    chartRight: Float,
    chartTop: Float,
    chartBottom: Float,
    minorColor: Color,
    majorColor: Color
) {
    val minorStep = 8.dp.toPx()

    var x = chartLeft
    var index = 0
    while (x <= chartRight) {
        val isMajor = index % 5 == 0
        drawLine(
            color = if (isMajor) majorColor else minorColor,
            start = Offset(x, chartTop),
            end = Offset(x, chartBottom),
            strokeWidth = if (isMajor) 1.dp.toPx() else 0.5.dp.toPx()
        )
        x += minorStep
        index++
    }

    var y = chartTop
    index = 0
    while (y <= chartBottom) {
        val isMajor = index % 5 == 0
        drawLine(
            color = if (isMajor) majorColor else minorColor,
            start = Offset(chartLeft, y),
            end = Offset(chartRight, y),
            strokeWidth = if (isMajor) 1.dp.toPx() else 0.5.dp.toPx()
        )
        y += minorStep
        index++
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawYAxisLabels(
    chartLeft: Float,
    chartTop: Float,
    chartBottom: Float,
    voltageMaxAbs: Float,
    labelColor: Color
) {
    val paint = axisLabelPaint(labelColor)
    val values = listOf(voltageMaxAbs, 0f, -voltageMaxAbs)
    val chartHeight = chartBottom - chartTop

    values.forEach { value ->
        val y = chartTop + ((voltageMaxAbs - value) / (voltageMaxAbs * 2f)) * chartHeight
        drawContext.canvas.nativeCanvas.drawText(
            formatAxisValue(value),
            2.dp.toPx(),
            y + 4.dp.toPx(),
            paint
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawXAxisLabels(
    chartLeft: Float,
    chartRight: Float,
    chartBottom: Float,
    visibleTimeMinMs: Float,
    visibleTimeMaxMs: Float,
    labelColor: Color
) {
    val paint = axisLabelPaint(labelColor)
    val chartWidth = chartRight - chartLeft
    val startSeconds = visibleTimeMinMs / 1000f
    val endSeconds = visibleTimeMaxMs / 1000f
    val firstTick = ceil(startSeconds).toInt()
    val lastTick = floor(endSeconds).toInt()

    if (lastTick < firstTick) return

    for (tick in firstTick..lastTick) {
        val x = chartLeft + ((tick - startSeconds) / (endSeconds - startSeconds)) * chartWidth
        drawLine(
            color = labelColor.copy(alpha = 0.55f),
            start = Offset(x, chartBottom),
            end = Offset(x, chartBottom + 4.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
        drawContext.canvas.nativeCanvas.drawText(
            "${tick}s",
            x - 8.dp.toPx(),
            chartBottom + 18.dp.toPx(),
            paint
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.axisLabelPaint(color: Color): Paint {
    return Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        textSize = 10.dp.toPx()
    }
}

private fun formatAxisValue(value: Float): String {
    return if (abs(value) >= 1f) {
        value.toInt().toString()
    } else {
        String.format("%.1f", value)
    }
}

private const val MIN_ZOOM_X = 1f
private const val MAX_ZOOM_X = 8f
private const val MIN_VOLTAGE_RANGE_MV = 1.0
