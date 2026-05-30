package com.rimuru.android.rhythmlens.ui.app.features.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.domain.model.UserRole
import com.rimuru.android.rhythmlens.ui.theme.RhythmSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.profile))
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
                ProfileInfoCard(state = state)
            }

            item {
                RoleSwitcherCard(
                    currentRole = state.role,
                    isRoleChanging = state.isRoleChanging,
                    onRoleSelected = { role ->
                        onEvent(ProfileEvent.RoleSelected(role))
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileInfoCard(
    state: ProfileUiState
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
                text = stringResource(R.string.profile_account),
                style = MaterialTheme.typography.titleMedium
            )

            ProfileInfoRow(
                title = stringResource(R.string.profile_full_name),
                value = state.fullName.ifBlank { stringResource(R.string.not_specified) }
            )

            ProfileInfoRow(
                title = stringResource(R.string.profile_email),
                value = state.email.ifBlank { stringResource(R.string.not_specified) }
            )

            ProfileInfoRow(
                title = stringResource(R.string.profile_current_role),
                value = state.role.label()
            )

            ProfileInfoRow(
                title = stringResource(R.string.profile_selected_patient),
                value = state.selectedPatientId ?: stringResource(R.string.not_specified)
            )
        }
    }
}

@Composable
private fun RoleSwitcherCard(
    currentRole: UserRole?,
    isRoleChanging: Boolean,
    onRoleSelected: (UserRole) -> Unit
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
                text = stringResource(R.string.profile_dev_role_switcher),
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = stringResource(R.string.profile_dev_role_switcher_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(RhythmSpacing.Small)
            ) {
                FilterChip(
                    selected = currentRole == UserRole.PATIENT,
                    enabled = !isRoleChanging,
                    onClick = { onRoleSelected(UserRole.PATIENT) },
                    label = {
                        Text(text = stringResource(R.string.role_patient))
                    }
                )

                FilterChip(
                    selected = currentRole == UserRole.DOCTOR,
                    enabled = !isRoleChanging,
                    onClick = { onRoleSelected(UserRole.DOCTOR) },
                    label = {
                        Text(text = stringResource(R.string.role_doctor))
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
    title: String,
    value: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(RhythmSpacing.ExtraSmall)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }

    Spacer(modifier = Modifier.height(RhythmSpacing.ExtraSmall))
}

@Composable
private fun UserRole?.label(): String {
    return when (this) {
        UserRole.PATIENT -> stringResource(R.string.role_patient)
        UserRole.DOCTOR -> stringResource(R.string.role_doctor)
        null -> stringResource(R.string.not_specified)
    }
}
