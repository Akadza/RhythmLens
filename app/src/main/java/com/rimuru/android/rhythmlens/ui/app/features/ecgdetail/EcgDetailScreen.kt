package com.rimuru.android.rhythmlens.ui.app.features.ecgdetail

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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.domain.model.UserRole
import com.rimuru.android.rhythmlens.ui.app.features.ecgdetail.components.AiAnalysisCard
import com.rimuru.android.rhythmlens.ui.app.features.ecgdetail.components.EcgLeadChart
import com.rimuru.android.rhythmlens.ui.theme.RhythmSize
import com.rimuru.android.rhythmlens.ui.theme.RhythmSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcgDetailScreen(
    state: EcgDetailUiState,
    onEvent: (EcgDetailEvent) -> Unit,
    modifier: Modifier = Modifier
) {
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
                    IconButton(onClick = { onEvent(EcgDetailEvent.BackClicked) }) {
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
                        onClick = { onEvent(EcgDetailEvent.CompareClicked) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(R.string.compare))
                    }

                    Button(
                        onClick = { onEvent(EcgDetailEvent.ExportClicked) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(R.string.export))
                    }

                    Box {
                        IconButton(onClick = { isMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.more)
                            )
                        }

                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.synthetic_ecg)) },
                                onClick = {
                                    isMenuExpanded = false
                                    onEvent(EcgDetailEvent.SyntheticClicked)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.doctor_conclusion)) },
                                onClick = {
                                    isMenuExpanded = false
                                    onEvent(EcgDetailEvent.DoctorConclusionClicked)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_record)) },
                                onClick = {
                                    isMenuExpanded = false
                                    onEvent(EcgDetailEvent.DeleteClicked)
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
            item { AiAnalysisCard(probabilities = state.probabilities) }

            item { SignalInfoCard(signalInfo = state.signalInfo) }

            item {
                SignalModeSelector(
                    selectedMode = state.signalMode,
                    onModeSelected = { mode ->
                        onEvent(EcgDetailEvent.SignalModeChanged(mode))
                    }
                )
            }

            item {
                LeadsCard(
                    leads = state.leads,
                    signalMode = state.signalMode
                )
            }

            item {
                DoctorConclusionCard(
                    conclusion = state.doctorConclusion,
                    currentUserRole = state.currentUserRole,
                    onEditClick = { onEvent(EcgDetailEvent.DoctorConclusionEditClicked) },
                    onTextChange = { text -> onEvent(EcgDetailEvent.DoctorConclusionTextChanged(text)) },
                    onSaveClick = { onEvent(EcgDetailEvent.DoctorConclusionSaveClicked) },
                    onCancelClick = { onEvent(EcgDetailEvent.DoctorConclusionCancelClicked) }
                )
            }
        }
    }

    if (state.isDeleteDialogVisible) {
        DeleteEcgDialog(
            isDeleting = state.isDeleting,
            onConfirm = { onEvent(EcgDetailEvent.DeleteConfirmed) },
            onDismiss = { onEvent(EcgDetailEvent.DeleteDismissed) }
        )
    }
}

@Composable
private fun DeleteEcgDialog(
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isDeleting) {
                onDismiss()
            }
        },
        title = {
            Text(text = stringResource(R.string.delete_ecg_dialog_title))
        },
        text = {
            Text(text = stringResource(R.string.delete_ecg_dialog_body))
        },
        confirmButton = {
            TextButton(
                enabled = !isDeleting,
                onClick = onConfirm
            ) {
                Text(text = stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isDeleting,
                onClick = onDismiss
            ) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
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
    selectedMode: SignalModeUi,
    onModeSelected: (SignalModeUi) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RhythmSpacing.Small)
    ) {
        FilterChip(
            selected = selectedMode == SignalModeUi.Full,
            onClick = {
                onModeSelected(SignalModeUi.Full)
            },
            label = {
                Text(
                    text = stringResource(R.string.signal_mode_full)
                )
            }
        )

        FilterChip(
            selected = selectedMode == SignalModeUi.DigitizedOnly,
            onClick = {
                onModeSelected(SignalModeUi.DigitizedOnly)
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
    signalMode: SignalModeUi
) {
    val visibleLeads = when (signalMode) {
        SignalModeUi.Full -> leads
        SignalModeUi.DigitizedOnly -> leads.filter { lead ->
            lead.origin == LeadOriginUi.Digitized || lead.origin == LeadOriginUi.Mixed
        }
    }

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
                    SignalModeUi.Full -> {
                        stringResource(R.string.signal_mode_full_description)
                    }

                    SignalModeUi.DigitizedOnly -> {
                        stringResource(R.string.signal_mode_digitized_description)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            visibleLeads.forEach { lead ->
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
            EcgLeadChart(
                leadName = lead.name,
                points = lead.points,
                segments = lead.segments,
                origin = lead.origin,
                modifier = Modifier.fillMaxSize()
            )
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
private fun DoctorConclusionCard(
    conclusion: DoctorConclusionUi,
    currentUserRole: UserRole?,
    onEditClick: () -> Unit,
    onTextChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val canEdit = currentUserRole == UserRole.DOCTOR

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.doctor_conclusion),
                    style = MaterialTheme.typography.titleMedium
                )

                if (canEdit && !conclusion.isEditing) {
                    TextButton(onClick = onEditClick) {
                        Text(
                            text = if (conclusion.text.isBlank()) {
                                stringResource(R.string.add)
                            } else {
                                stringResource(R.string.edit)
                            }
                        )
                    }
                }
            }

            if (conclusion.updatedAt != null && !conclusion.isEditing) {
                Text(
                    text = stringResource(R.string.doctor_conclusion_updated_template, conclusion.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (conclusion.isEditing) {
                OutlinedTextField(
                    value = conclusion.draftText,
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    label = {
                        Text(text = stringResource(R.string.doctor_conclusion_text_label))
                    },
                    placeholder = {
                        Text(text = stringResource(R.string.doctor_conclusion_text_hint))
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RhythmSpacing.Small)
                ) {
                    TextButton(
                        enabled = !conclusion.isSaving,
                        onClick = onCancelClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(R.string.cancel))
                    }

                    Button(
                        enabled = !conclusion.isSaving && conclusion.draftText.isNotBlank(),
                        onClick = onSaveClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (conclusion.isSaving) {
                                stringResource(R.string.processing)
                            } else {
                                stringResource(R.string.save)
                            }
                        )
                    }
                }
            } else {
                Text(
                    text = conclusion.text.ifBlank {
                        stringResource(R.string.doctor_conclusion_empty)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (conclusion.text.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}
