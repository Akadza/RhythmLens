package com.rimuru.android.rhythmlens.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.ui.theme.RhythmSize
import com.rimuru.android.rhythmlens.ui.theme.RhythmSpacing

data class HomeUiState(
    val userName: String,
    val totalRecords: Int,
    val linkedDoctorCount: Int,
    val lastRecord: LastEcgUi?
)

data class LastEcgUi(
    val id: String,
    val date: String,
    val mainResult: String,
    val probability: Int,
    val digitizedLeads: Int,
    val reconstructedLeads: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    state: HomeUiState = sampleHomeState(),
    onScanClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onImportClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isSheetVisible by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isSheetVisible = true },
                modifier = Modifier
                    .size(RhythmSize.Fab)
                    .padding(bottom = RhythmSpacing.Small),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
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
                WelcomeCard(userName = state.userName)
            }

            item {
                LastEcgCard(record = state.lastRecord)
            }

            item {
                StatsCard(
                    totalRecords = state.totalRecords,
                    linkedDoctorCount = state.linkedDoctorCount
                )
            }

            item {
                Text(
                    text = stringResource(R.string.home_add_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RhythmSpacing.Medium)
                )
            }

            item {
                Spacer(
                    modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)
                )
            }
        }
    }

    if (isSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { isSheetVisible = false },
            sheetState = sheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle()
            }
        ) {
            AddEcgBottomSheetContent(
                onScanClick = {
                    isSheetVisible = false
                    onScanClick()
                },
                onGalleryClick = {
                    isSheetVisible = false
                    onGalleryClick()
                },
                onImportClick = {
                    isSheetVisible = false
                    onImportClick()
                }
            )
        }
    }
}

@Composable
private fun WelcomeCard(
    userName: String
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
                style = MaterialTheme.typography.titleLarge
            )

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
    record: LastEcgUi?
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
                text = stringResource(R.string.last_ecg),
                style = MaterialTheme.typography.titleMedium
            )

            if (record == null) {
                Text(
                    text = stringResource(R.string.last_ecg_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = record.date,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = stringResource(
                        R.string.result_probability_template,
                        record.mainResult,
                        record.probability
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = stringResource(
                        R.string.signal_origin_template,
                        record.digitizedLeads,
                        record.reconstructedLeads
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
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
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AddEcgBottomSheetContent(
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
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.take_photo))
        }

        FilledTonalButton(
            onClick = onGalleryClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.choose_from_gallery))
        }

        FilledTonalButton(
            onClick = onImportClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.import_file))
        }
    }
}

@Composable
private fun sampleHomeState(): HomeUiState {
    return HomeUiState(
        userName = stringResource(R.string.sample_user_name),
        totalRecords = 3,
        linkedDoctorCount = 0,
        lastRecord = LastEcgUi(
            id = "ecg-1",
            date = stringResource(R.string.sample_date_1),
            mainResult = stringResource(R.string.sample_result_afib),
            probability = 50,
            digitizedLeads = 8,
            reconstructedLeads = 4
        )
    )
}