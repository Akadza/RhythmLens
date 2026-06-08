package com.rimuru.android.rhythmlens.ui.app.features.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.ui.theme.RhythmSpacing
import com.rimuru.android.rhythmlens.ui.theme.WarningAmber

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.date,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                StatusIndicator(status = item.status)
            }

            Text(
                text = item.patientName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RhythmSpacing.Medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        R.string.signal_origin_template,
                        item.digitizedLeads,
                        item.reconstructedLeads
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (item.hasDoctorConclusion) {
                    Text(
                        text = stringResource(R.string.doctor_conclusion_exists),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIndicator(status: EcgProcessingStatusUi) {
    val color = when (status) {
        EcgProcessingStatusUi.Processed -> MaterialTheme.colorScheme.tertiary
        EcgProcessingStatusUi.Error -> MaterialTheme.colorScheme.error
        EcgProcessingStatusUi.Draft,
        EcgProcessingStatusUi.Uploading,
        EcgProcessingStatusUi.Digitizing,
        EcgProcessingStatusUi.Completing,
        EcgProcessingStatusUi.Analyzing -> WarningAmber
    }
    val label = when (status) {
        EcgProcessingStatusUi.Processed -> stringResource(R.string.status_ready_short)
        EcgProcessingStatusUi.Error -> stringResource(R.string.status_error_short)
        else -> stringResource(R.string.status_wait_short)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(RhythmSpacing.ExtraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = color, shape = CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
