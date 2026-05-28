package com.rimuru.android.rhythmlens.ui.screens.ecg

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.ui.screens.ecg.components.AiAnalysisCard
import com.rimuru.android.rhythmlens.ui.screens.ecg.components.DiagnosisProbabilityUi
import com.rimuru.android.rhythmlens.ui.theme.RhythmSize
import com.rimuru.android.rhythmlens.ui.theme.RhythmSpacing

data class EcgDetailUiState(
    val ecgId: String,
    val date: String,
    val probabilities: List<DiagnosisProbabilityUi>,
    val signalInfo: SignalInfoUi,
    val leads: List<LeadSummaryUi>
)

data class SignalInfoUi(
    val duration: String,
    val samplingRate: String,
    val digitizedLeads: Int,
    val reconstructedLeads: Int,
    val source: String,
    val quality: String
)

data class LeadSummaryUi(
    val name: String,
    val origin: LeadOriginUi
)

enum class LeadOriginUi {
    Digitized,
    Reconstructed,
    Mixed
}

private enum class SignalMode {
    Full,
    DigitizedOnly
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcgDetailScreen(
    ecgId: String,
    onBackClick: () -> Unit,
    onCompareClick: () -> Unit,
    onSyntheticClick: () -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: EcgDetailUiState = sampleEcgDetailState(ecgId)
) {
    var signalMode by remember {
        mutableStateOf(SignalMode.Full)
    }

    var isMenuExpanded by remember {
        mutableStateOf(false)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            R.string.ecg_detail_title_template,
                            state.date
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RhythmSpacing.Medium),
                    horizontalArrangement = Arrangement.spacedBy(RhythmSpacing.Small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onCompareClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.compare)
                        )
                    }

                    Button(
                        onClick = onExportClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.export)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = {
                                isMenuExpanded = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.more)
                            )
                        }

                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = {
                                isMenuExpanded = false
                            }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.synthetic_ecg)
                                    )
                                },
                                onClick = {
                                    isMenuExpanded = false
                                    onSyntheticClick()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.doctor_conclusion)
                                    )
                                },
                                onClick = {
                                    isMenuExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.delete_record)
                                    )
                                },
                                onClick = {
                                    isMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(RhythmSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Large)
        ) {
            item {
                AiAnalysisCard(
                    probabilities = state.probabilities
                )
            }

            item {
                SignalInfoCard(
                    signalInfo = state.signalInfo
                )
            }

            item {
                SignalModeSelector(
                    selectedMode = signalMode,
                    onModeSelected = { selectedMode ->
                        signalMode = selectedMode
                    }
                )
            }

            item {
                LeadsCard(
                    leads = state.leads,
                    signalMode = signalMode
                )
            }

            item {
                DoctorConclusionPreview()
            }
        }
    }
}

@Composable
private fun SignalInfoCard(
    signalInfo: SignalInfoUi
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(RhythmSpacing.ExtraLarge),
            verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Medium)
        ) {
            Text(
                text = stringResource(R.string.signal_data),
                style = MaterialTheme.typography.titleMedium
            )

            SignalInfoRow(
                title = stringResource(R.string.signal_format),
                value = stringResource(R.string.signal_format_12_leads)
            )

            SignalInfoRow(
                title = stringResource(R.string.signal_duration),
                value = signalInfo.duration
            )

            SignalInfoRow(
                title = stringResource(R.string.signal_sampling_rate),
                value = signalInfo.samplingRate
            )

            SignalInfoRow(
                title = stringResource(R.string.signal_digitized),
                value = stringResource(
                    R.string.leads_count_template,
                    signalInfo.digitizedLeads
                )
            )

            SignalInfoRow(
                title = stringResource(R.string.signal_reconstructed),
                value = stringResource(
                    R.string.leads_count_template,
                    signalInfo.reconstructedLeads
                )
            )

            SignalInfoRow(
                title = stringResource(R.string.signal_source),
                value = signalInfo.source
            )

            SignalInfoRow(
                title = stringResource(R.string.signal_quality),
                value = signalInfo.quality
            )
        }
    }
}

@Composable
private fun SignalInfoRow(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SignalModeSelector(
    selectedMode: SignalMode,
    onModeSelected: (SignalMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RhythmSpacing.Small)
    ) {
        FilterChip(
            selected = selectedMode == SignalMode.Full,
            onClick = {
                onModeSelected(SignalMode.Full)
            },
            label = {
                Text(
                    text = stringResource(R.string.signal_mode_full)
                )
            }
        )

        FilterChip(
            selected = selectedMode == SignalMode.DigitizedOnly,
            onClick = {
                onModeSelected(SignalMode.DigitizedOnly)
            },
            label = {
                Text(
                    text = stringResource(R.string.signal_mode_digitized_only)
                )
            }
        )
    }
}

@Composable
private fun LeadsCard(
    leads: List<LeadSummaryUi>,
    signalMode: SignalMode
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(RhythmSpacing.ExtraLarge),
            verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Medium)
        ) {
            Text(
                text = stringResource(R.string.twelve_leads),
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = when (signalMode) {
                    SignalMode.Full -> {
                        stringResource(R.string.signal_mode_full_description)
                    }

                    SignalMode.DigitizedOnly -> {
                        stringResource(R.string.signal_mode_digitized_description)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            leads.forEach { lead ->
                LeadItem(lead = lead)
            }
        }
    }
}

@Composable
private fun LeadItem(
    lead: LeadSummaryUi
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = lead.name,
                style = MaterialTheme.typography.titleMedium
            )

            AssistChip(
                onClick = {},
                label = {
                    Text(
                        text = lead.origin.label()
                    )
                }
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(RhythmSize.LeadPreviewHeight),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(
                        R.string.lead_chart_placeholder_template,
                        lead.name
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(RhythmSpacing.Large)
                )
            }
        }
    }
}

@Composable
private fun LeadOriginUi.label(): String {
    return when (this) {
        LeadOriginUi.Digitized -> {
            stringResource(R.string.lead_origin_digitized)
        }

        LeadOriginUi.Reconstructed -> {
            stringResource(R.string.lead_origin_reconstructed)
        }

        LeadOriginUi.Mixed -> {
            stringResource(R.string.lead_origin_mixed)
        }
    }
}

@Composable
private fun DoctorConclusionPreview() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(RhythmSpacing.ExtraLarge),
            verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Small)
        ) {
            Text(
                text = stringResource(R.string.doctor_conclusion),
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = stringResource(R.string.doctor_conclusion_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun sampleEcgDetailState(
    ecgId: String
): EcgDetailUiState {
    return EcgDetailUiState(
        ecgId = ecgId,
        date = stringResource(R.string.sample_date_1),
        probabilities = listOf(
            DiagnosisProbabilityUi(
                title = stringResource(R.string.sample_result_afib),
                code = stringResource(R.string.sample_code_afib),
                probability = 50
            ),
            DiagnosisProbabilityUi(
                title = stringResource(R.string.sample_result_st_changes),
                code = null,
                probability = 20
            ),
            DiagnosisProbabilityUi(
                title = stringResource(R.string.sample_result_rbbb),
                code = null,
                probability = 10
            ),
            DiagnosisProbabilityUi(
                title = stringResource(R.string.sample_result_normal),
                code = null,
                probability = 5
            ),
            DiagnosisProbabilityUi(
                title = stringResource(R.string.sample_result_infarction),
                code = null,
                probability = 5
            )
        ),
        signalInfo = SignalInfoUi(
            duration = stringResource(R.string.sample_duration_10s),
            samplingRate = stringResource(R.string.sample_sampling_rate_500hz),
            digitizedLeads = 8,
            reconstructedLeads = 4,
            source = stringResource(R.string.source_photo),
            quality = stringResource(R.string.quality_medium)
        ),
        leads = listOf(
            LeadSummaryUi(
                name = stringResource(R.string.lead_i),
                origin = LeadOriginUi.Digitized
            ),
            LeadSummaryUi(
                name = stringResource(R.string.lead_ii),
                origin = LeadOriginUi.Digitized
            ),
            LeadSummaryUi(
                name = stringResource(R.string.lead_iii),
                origin = LeadOriginUi.Digitized
            ),
            LeadSummaryUi(
                name = stringResource(R.string.lead_avr),
                origin = LeadOriginUi.Digitized
            ),
            LeadSummaryUi(
                name = stringResource(R.string.lead_avl),
                origin = LeadOriginUi.Digitized
            ),
            LeadSummaryUi(
                name = stringResource(R.string.lead_avf),
                origin = LeadOriginUi.Digitized
            ),
            LeadSummaryUi(
                name = stringResource(R.string.lead_v1),
                origin = LeadOriginUi.Digitized
            ),
            LeadSummaryUi(
                name = stringResource(R.string.lead_v2),
                origin = LeadOriginUi.Digitized
            ),
            LeadSummaryUi(
                name = stringResource(R.string.lead_v3),
                origin = LeadOriginUi.Reconstructed
            ),
            LeadSummaryUi(
                name = stringResource(R.string.lead_v4),
                origin = LeadOriginUi.Reconstructed
            ),
            LeadSummaryUi(
                name = stringResource(R.string.lead_v5),
                origin = LeadOriginUi.Reconstructed
            ),
            LeadSummaryUi(
                name = stringResource(R.string.lead_v6),
                origin = LeadOriginUi.Reconstructed
            )
        )
    )
}