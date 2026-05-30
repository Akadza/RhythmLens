package com.rimuru.android.rhythmlens.ui.app.features.patients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.ui.components.EmptyState
import com.rimuru.android.rhythmlens.ui.theme.RhythmSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientsScreen(
    state: PatientsUiState,
    onEvent: (PatientsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.patients))
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(RhythmSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Large)
        ) {
            item {
                PatientsHeaderCard(
                    isAddingPatient = state.isAddingPatient,
                    onAddTestPatientClick = {
                        onEvent(PatientsEvent.AddTestPatientClicked)
                    }
                )
            }

            if (state.errorMessage != null) {
                item {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (!state.isLoading && state.patients.isEmpty()) {
                item {
                    EmptyState(
                        title = stringResource(R.string.patients_empty_title),
                        body = stringResource(R.string.patients_empty_body)
                    )
                }
            }

            items(
                items = state.patients,
                key = { patient -> patient.id }
            ) { patient ->
                PatientListItem(
                    patient = patient,
                    onClick = {
                        onEvent(PatientsEvent.PatientClicked(patient.id))
                    }
                )
            }
        }
    }
}

@Composable
private fun PatientsHeaderCard(
    isAddingPatient: Boolean,
    onAddTestPatientClick: () -> Unit
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
                text = stringResource(R.string.patients_header_title),
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = stringResource(R.string.patients_header_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onAddTestPatientClick,
                enabled = !isAddingPatient,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isAddingPatient) {
                        stringResource(R.string.processing)
                    } else {
                        stringResource(R.string.add_test_patient)
                    }
                )
            }
        }
    }
}

@Composable
private fun PatientListItem(
    patient: PatientItemUi,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (patient.isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
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
                    text = patient.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                if (patient.isSelected) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(text = stringResource(R.string.patient_selected))
                        }
                    )
                }
            }

            Text(
                text = patient.age,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = stringResource(R.string.patient_invite_code_template, patient.inviteCode),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
