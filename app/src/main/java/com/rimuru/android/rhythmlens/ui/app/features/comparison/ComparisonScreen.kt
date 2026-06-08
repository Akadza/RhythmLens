package com.rimuru.android.rhythmlens.ui.app.features.comparison

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.ui.app.features.comparison.components.EcgComparisonChart
import com.rimuru.android.rhythmlens.ui.theme.RhythmSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonScreen(
    state: ComparisonUiState,
    onEvent: (ComparisonEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.screen_comparison))
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(ComparisonEvent.BackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(RhythmSpacing.Large),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.padding(innerPadding),
                    contentPadding = PaddingValues(RhythmSpacing.Large),
                    verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Large)
                ) {
                    item {
                        SummaryCard(summary = state.summary)
                    }

                    item {
                        RecordSelectorCard(
                            baseTitle = state.baseTitle,
                            candidates = state.candidates,
                            selectedComparedEcgId = state.selectedComparedEcgId,
                            onCandidateClick = { ecgId ->
                                onEvent(ComparisonEvent.ComparedRecordSelected(ecgId))
                            }
                        )
                    }

                    item {
                        ComparisonModeSelector(
                            selectedMode = state.signalMode,
                            onModeSelected = { mode ->
                                onEvent(ComparisonEvent.SignalModeChanged(mode))
                            }
                        )
                    }

                    item {
                        LegendCard()
                    }

                    if (state.selectedComparedEcgId != null) {
                        item {
                            LeadsComparisonCard(
                                leads = state.leads,
                                mode = state.signalMode
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: ComparisonSummaryUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(RhythmSpacing.ExtraLarge),
            verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Small)
        ) {
            Text(
                text = summary.title.ifBlank { stringResource(R.string.comparison_summary_title) },
                style = MaterialTheme.typography.titleMedium
            )
            summary.points.forEach { point ->
                Text(
                    text = "• $point",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecordSelectorCard(
    baseTitle: String,
    candidates: List<ComparisonCandidateUi>,
    selectedComparedEcgId: String?,
    onCandidateClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(RhythmSpacing.ExtraLarge),
            verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Medium)
        ) {
            Text(
                text = stringResource(R.string.comparison_records_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.comparison_base_record_template, baseTitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (candidates.isEmpty()) {
                Text(
                    text = stringResource(R.string.comparison_no_candidates),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(RhythmSpacing.Small)
                ) {
                    items(candidates, key = { candidate -> candidate.id }) { candidate ->
                        FilterChip(
                            selected = candidate.id == selectedComparedEcgId,
                            onClick = { onCandidateClick(candidate.id) },
                            label = {
                                Column {
                                    Text(text = candidate.title)
                                    Text(
                                        text = candidate.subtitle,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonModeSelector(
    selectedMode: ComparisonSignalModeUi,
    onModeSelected: (ComparisonSignalModeUi) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RhythmSpacing.Small)
    ) {
        FilterChip(
            selected = selectedMode == ComparisonSignalModeUi.Full,
            onClick = { onModeSelected(ComparisonSignalModeUi.Full) },
            label = { Text(text = stringResource(R.string.signal_mode_full)) }
        )
        FilterChip(
            selected = selectedMode == ComparisonSignalModeUi.DigitizedOnly,
            onClick = { onModeSelected(ComparisonSignalModeUi.DigitizedOnly) },
            label = { Text(text = stringResource(R.string.signal_mode_digitized_only)) }
        )
    }
}

@Composable
private fun LegendCard() {
    val baseColor = MaterialTheme.colorScheme.primary
    val comparedColor = MaterialTheme.colorScheme.tertiary
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(RhythmSpacing.ExtraLarge),
            horizontalArrangement = Arrangement.spacedBy(RhythmSpacing.Large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(
                color = baseColor,
                text = stringResource(R.string.comparison_legend_base)
            )
            LegendItem(
                color = comparedColor,
                text = stringResource(R.string.comparison_legend_compared)
            )
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    text: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(RhythmSpacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LeadsComparisonCard(
    leads: List<ComparisonLeadUi>,
    mode: ComparisonSignalModeUi
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(RhythmSpacing.ExtraLarge),
            verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Medium)
        ) {
            Text(
                text = stringResource(R.string.twelve_leads),
                style = MaterialTheme.typography.titleMedium
            )
            leads.forEach { lead ->
                LeadComparisonItem(
                    lead = lead,
                    mode = mode
                )
            }
        }
    }
}

@Composable
private fun LeadComparisonItem(
    lead: ComparisonLeadUi,
    mode: ComparisonSignalModeUi
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Small)
    ) {
        Text(
            text = lead.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(144.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            EcgComparisonChart(
                leadName = lead.name,
                basePoints = lead.basePoints,
                comparedPoints = lead.comparedPoints,
                baseSegments = lead.baseSegments,
                comparedSegments = lead.comparedSegments,
                mode = mode,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
