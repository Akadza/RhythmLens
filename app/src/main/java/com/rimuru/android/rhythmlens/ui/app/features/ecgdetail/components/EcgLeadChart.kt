package com.rimuru.android.rhythmlens.ui.app.features.ecgdetail.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.domain.model.EcgPoint
import com.rimuru.android.rhythmlens.ui.app.features.ecgdetail.LeadOriginUi
import com.rimuru.android.rhythmlens.ui.theme.RhythmSpacing
import kotlin.math.max
import kotlin.math.min

@Composable
fun EcgLeadChart(
    leadName: String,
    points: List<EcgPoint>,
    origin: LeadOriginUi,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val gridMinorColor = colorScheme.outlineVariant.copy(alpha = 0.32f)
    val gridMajorColor = colorScheme.outline.copy(alpha = 0.42f)
    val signalColor = when (origin) {
        LeadOriginUi.Digitized -> colorScheme.primary
        LeadOriginUi.Reconstructed -> colorScheme.tertiary
        LeadOriginUi.Mixed -> colorScheme.secondary
    }
    val axisColor = colorScheme.onSurfaceVariant.copy(alpha = 0.72f)

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
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartLeft = 38.dp.toPx()
                val chartRight = size.width - 10.dp.toPx()
                val chartTop = 10.dp.toPx()
                val chartBottom = size.height - 24.dp.toPx()
                val chartWidth = max(1f, chartRight - chartLeft)
                val chartHeight = max(1f, chartBottom - chartTop)
                val centerY = chartTop + chartHeight / 2f

                val timeMin = points.first().timeMs.toFloat()
                val timeMax = points.last().timeMs.toFloat().coerceAtLeast(timeMin + 1f)
                val voltageMaxAbs = max(
                    1.0,
                    points.maxOf { kotlin.math.abs(it.voltageMv) }
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

                val path = Path()
                points.forEachIndexed { index, point ->
                    val x = chartLeft + ((point.timeMs - timeMin) / (timeMax - timeMin)) * chartWidth
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
    val majorStep = minorStep * 5f

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
