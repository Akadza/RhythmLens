package com.rimuru.android.rhythmlens.ui.app.features.ecgdetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    var isExplanationVisible by remember {
        mutableStateOf(false)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.ai_analysis_result),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { isExplanationVisible = true }) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = stringResource(R.string.ai_result_explanation)
                    )
                }
            }

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

    if (isExplanationVisible) {
        DiagnosisExplanationDialog(
            probabilities = probabilities,
            onDismiss = { isExplanationVisible = false }
        )
    }
}

@Composable
private fun DiagnosisExplanationDialog(
    probabilities: List<DiagnosisProbabilityUi>,
    onDismiss: () -> Unit
) {
    val topItems = probabilities
        .sortedByDescending { item -> item.probability }
        .take(5)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.ai_result_explanation))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Small)
            ) {
                Text(
                    text = stringResource(R.string.ai_result_explanation_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                topItems.forEach { item ->
                    Text(
                        text = "${item.title}: ${item.explanation()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.close))
            }
        }
    )
}

private fun DiagnosisProbabilityUi.explanation(): String {
    val value = "${title} ${code.orEmpty()}".lowercase()
    return when {
        "normal" in value || "норма" in value -> "признаки без выраженных отклонений в рамках используемой модели."
        "af" in value || "фибрил" in value -> "возможные признаки нарушения ритма по типу фибрилляции предсердий."
        "st" in value -> "возможные изменения сегмента ST, требующие врачебной проверки."
        "block" in value || "блок" in value || "rbbb" in value || "lbbb" in value -> "возможные признаки нарушения внутрижелудочковой проводимости."
        "mi" in value || "infar" in value || "инфар" in value -> "возможные признаки ишемических или постинфарктных изменений."
        "hypert" in value || "гиперт" in value -> "возможные признаки гипертрофии отделов сердца."
        else -> "класс автоматического анализа ЭКГ; итоговая интерпретация требует проверки врачом."
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
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
