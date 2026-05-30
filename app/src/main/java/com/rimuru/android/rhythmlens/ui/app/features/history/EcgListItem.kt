package com.rimuru.android.rhythmlens.ui.app.features.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.ui.theme.RhythmSpacing

data class EcgHistoryItemUi(
    val id: String,
    val date: String,
    val patientName: String,
    val mainResult: String,
    val probability: Int,
    val digitizedLeads: Int,
    val reconstructedLeads: Int,
    val status: EcgProcessingStatusUi,
    val hasDoctorConclusion: Boolean
)

enum class EcgProcessingStatusUi {
    Draft,
    Uploading,
    Digitizing,
    Completing,
    Analyzing,
    Processed,
    Error
}

@Composable
fun EcgListItem(
    item: EcgHistoryItemUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = item.status != EcgProcessingStatusUi.Draft
            ) {
                onClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(RhythmSpacing.ExtraLarge),
            verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Small)
        ) {
            Text(
                text = item.date,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = item.patientName,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = if (item.status == EcgProcessingStatusUi.Processed) {
                    stringResource(
                        R.string.result_probability_template,
                        item.mainResult,
                        item.probability
                    )
                } else {
                    item.mainResult
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(RhythmSpacing.Small)
            ) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = stringResource(
                                R.string.signal_origin_template,
                                item.digitizedLeads,
                                item.reconstructedLeads
                            )
                        )
                    }
                )

                AssistChip(
                    onClick = {},
                    label = {
                        Text(text = item.status.label())
                    }
                )
            }

            AssistChip(
                onClick = {},
                label = {
                    Text(
                        text = if (item.hasDoctorConclusion) {
                            stringResource(R.string.doctor_conclusion_exists)
                        } else {
                            stringResource(R.string.doctor_conclusion_missing)
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun EcgProcessingStatusUi.label(): String {
    return when (this) {
        EcgProcessingStatusUi.Draft -> stringResource(R.string.status_draft)
        EcgProcessingStatusUi.Uploading -> stringResource(R.string.status_uploading)
        EcgProcessingStatusUi.Digitizing -> stringResource(R.string.status_digitizing)
        EcgProcessingStatusUi.Completing -> stringResource(R.string.status_completing)
        EcgProcessingStatusUi.Analyzing -> stringResource(R.string.status_analyzing)
        EcgProcessingStatusUi.Processed -> stringResource(R.string.status_processed)
        EcgProcessingStatusUi.Error -> stringResource(R.string.status_error)
    }
}
