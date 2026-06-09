package com.rimuru.android.rhythmlens.ui.app.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.ui.theme.RhythmSize
import com.rimuru.android.rhythmlens.ui.theme.RhythmSpacing
import com.rimuru.android.rhythmlens.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!state.isCreatingTestEcg) {
                        onEvent(HomeEvent.AddEcgClicked)
                    }
                },
                modifier = Modifier.size(RhythmSize.Fab),
                shape = CircleShape,
                containerColor = if (state.isCreatingTestEcg) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
                contentColor = if (state.isCreatingTestEcg) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onPrimary
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_ecg),
                    modifier = Modifier.size(RhythmSize.FabIcon)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = RhythmSpacing.ScreenHorizontal,
                top = RhythmSpacing.ScreenVertical,
                end = RhythmSpacing.ScreenHorizontal,
                bottom = RhythmSpacing.XXLarge
            ),
            verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Large)
        ) {
            item {
                WelcomeCard(
                    userName = state.userName,
                    selectedPatientName = state.selectedPatientName
                )
            }

            if (state.errorMessage != null) {
                item {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                LastEcgCard(
                    record = state.lastRecord,
                    isProcessingNewRecord = state.isCreatingTestEcg,
                    onClick = { ecgId ->
                        onEvent(HomeEvent.LastRecordClicked(ecgId))
                    }
                )
            }

            item {
                StatsCard(
                    totalRecords = state.totalRecords,
                    linkedDoctorCount = state.linkedDoctorCount
                )
            }

            item {
                Spacer(
                    modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)
                )
            }
        }
    }

    if (state.isAddEcgSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                onEvent(HomeEvent.AddEcgSheetDismissed)
            },
            sheetState = sheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle()
            }
        ) {
            AddEcgBottomSheetContent(
                isEnabled = !state.isCreatingTestEcg,
                onScanClick = {
                    onEvent(HomeEvent.ScanClicked)
                },
                onGalleryClick = {
                    onEvent(HomeEvent.GalleryClicked)
                },
                onImportClick = {
                    onEvent(HomeEvent.ImportClicked)
                }
            )
        }
    }
}

@Composable
private fun WelcomeCard(
    userName: String,
    selectedPatientName: String?
) {
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
                text = stringResource(R.string.home_greeting_template, userName),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!selectedPatientName.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.selected_patient_template, selectedPatientName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = stringResource(R.string.home_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LastEcgCard(
    record: LastEcgUi?,
    isProcessingNewRecord: Boolean,
    onClick: (String) -> Unit
) {
    val clickModifier = if (record != null && !isProcessingNewRecord) {
        Modifier.clickable { onClick(record.id) }
    } else {
        Modifier
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickModifier),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(RhythmSpacing.ExtraLarge),
            verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Small)
        ) {
            Text(
                text = stringResource(R.string.last_ecg),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (isProcessingNewRecord) {
                ProcessingRecordContent()
            } else if (record == null) {
                Text(
                    text = stringResource(R.string.last_ecg_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = record.date,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (record.probability == null) {
                        record.mainResult
                    } else {
                        stringResource(
                            R.string.result_probability_template,
                            record.mainResult,
                            record.probability
                        )
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = stringResource(
                        R.string.signal_origin_template,
                        record.digitizedLeads,
                        record.reconstructedLeads
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ProcessingRecordContent() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(RhythmSpacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = WarningAmber, shape = CircleShape)
        )
        Text(
            text = stringResource(R.string.status_processing),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    Text(
        text = stringResource(R.string.ecg_processing_placeholder),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun StatsCard(
    totalRecords: Int,
    linkedDoctorCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RhythmSpacing.ExtraLarge),
            horizontalArrangement = Arrangement.spacedBy(RhythmSpacing.Medium)
        ) {
            StatItem(
                title = stringResource(R.string.records_count),
                value = totalRecords.toString(),
                modifier = Modifier.weight(1f)
            )

            StatItem(
                title = stringResource(R.string.doctors_count),
                value = linkedDoctorCount.toString(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(RhythmSpacing.ExtraSmall)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AddEcgBottomSheetContent(
    isEnabled: Boolean,
    onScanClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onImportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = RhythmSpacing.XLarge,
                top = RhythmSpacing.Small,
                end = RhythmSpacing.XLarge,
                bottom = RhythmSpacing.XXLarge
            ),
        verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Medium)
    ) {
        Text(
            text = stringResource(R.string.add_ecg),
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = stringResource(R.string.add_ecg_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = onScanClick,
            enabled = isEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.take_photo))
        }

        FilledTonalButton(
            onClick = onGalleryClick,
            enabled = isEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.choose_from_gallery))
        }

        FilledTonalButton(
            onClick = onImportClick,
            enabled = isEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.import_file))
        }
    }
}
