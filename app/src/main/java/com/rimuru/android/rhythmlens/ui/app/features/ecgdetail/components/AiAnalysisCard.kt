package com.rimuru.android.rhythmlens.ui.screens.ecg.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.ui.theme.RhythmSize
import com.rimuru.android.rhythmlens.ui.theme.RhythmSpacing

data class DiagnosisProbabilityUi(
    val title: String,
    val code: String?,
    val probability: Int
)

@Composable
fun AiAnalysisCard(
    probabilities: List<DiagnosisProbabilityUi>,
    modifier: Modifier = Modifier
) {
    val mainResult = probabilities.maxByOrNull { item ->
        item.probability
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RhythmSpacing.ExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Large)
        ) {
            Text(
                text = stringResource(R.string.ai_analysis_result),
                style = MaterialTheme.typography.titleMedium
            )

            if (mainResult != null) {
                MainProbabilityIndicator(mainResult = mainResult)

                Text(
                    text = mainResult.title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.ai_result_requires_review),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Small)
            ) {
                Text(
                    text = stringResource(R.string.top_probable_classes),
                    style = MaterialTheme.typography.titleMedium
                )

                probabilities
                    .sortedByDescending { item -> item.probability }
                    .take(5)
                    .forEach { item ->
                        DiagnosisProgressItem(item = item)
                    }
            }
        }
    }
}

@Composable
private fun MainProbabilityIndicator(
    mainResult: DiagnosisProbabilityUi
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(RhythmSize.AnalysisIndicator)
    ) {
        CircularProgressIndicator(
            progress = {
                mainResult.probability / 100f
            },
            modifier = Modifier.size(RhythmSize.AnalysisIndicator),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = RhythmSize.AnalysisStroke,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.percent_template, mainResult.probability),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )

            if (mainResult.code != null) {
                Text(
                    text = mainResult.code,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DiagnosisProgressItem(
    item: DiagnosisProbabilityUi
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = stringResource(R.string.percent_template, item.probability),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(
            modifier = Modifier.width(RhythmSpacing.Medium)
        )

        LinearProgressIndicator(
            progress = {
                item.probability / 100f
            },
            modifier = Modifier.width(RhythmSize.ProbabilityBarWidth),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}