package com.rimuru.android.rhythmlens.ui.app.features.patients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.ui.theme.RhythmSpacing
import com.rimuru.android.rhythmlens.ui.theme.SecondaryTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientsScreen(
    state: PatientsUiState,
    onEvent: (PatientsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                modifier = Modifier.height(56.dp),
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
                    inviteCodeInput = state.inviteCodeInput,
                    isAttachingPatient = state.isAttachingPatient,
                    onInviteCodeChange = { value ->
                        onEvent(PatientsEvent.InviteCodeChanged(value))
                    },
                    onAttachPatientClick = {
                        onEvent(PatientsEvent.AttachPatientClicked)
                    }
                )
            }

            if (state.errorMessage != null) {
                item {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!state.isLoading && state.patients.isEmpty()) {
                item {
                    PatientsEmptyCard()
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
    inviteCodeInput: String,
    isAttachingPatient: Boolean,
    onInviteCodeChange: (String) -> Unit,
    onAttachPatientClick: () -> Unit
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
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = stringResource(R.string.patients_header_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = inviteCodeInput,
                onValueChange = onInviteCodeChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = {
                    Text(text = stringResource(R.string.patient_invite_code))
                },
                placeholder = {
                    Text(text = stringResource(R.string.patient_invite_code_hint))
                }
            )

            Button(
                onClick = onAttachPatientClick,
                enabled = !isAttachingPatient && inviteCodeInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isAttachingPatient) {
                        stringResource(R.string.processing)
                    } else {
                        stringResource(R.string.attach_patient_by_code)
                    }
                )
            }
        }
    }
}

@Composable
private fun PatientsEmptyCard() {
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
                text = stringResource(R.string.patients_empty_title),
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = stringResource(R.string.patients_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                SecondaryTeal.copy(alpha = 0.16f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(RhythmSpacing.ExtraLarge),
            verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Small)
        ) {
            Text(
                text = patient.fullName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = patient.age,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = stringResource(R.string.patient_invite_code_template, patient.inviteCode),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
