package com.rimuru.android.rhythmlens.ui.app.features.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.domain.model.UserRole
import com.rimuru.android.rhythmlens.ui.theme.RhythmSpacing

@Composable
fun AuthScreen(
    state: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(RhythmSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Large)
        ) {
            item {
                AuthCard(
                    state = state,
                    onEvent = onEvent
                )
            }
        }
    }
}

@Composable
private fun AuthCard(
    state: AuthUiState,
    onEvent: (AuthEvent) -> Unit
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
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = if (state.mode == AuthMode.Login) {
                    stringResource(R.string.auth_login_title)
                } else {
                    stringResource(R.string.auth_register_title)
                },
                style = MaterialTheme.typography.titleLarge
            )

            if (state.mode == AuthMode.Register) {
                OutlinedTextField(
                    value = state.fullName,
                    onValueChange = { value -> onEvent(AuthEvent.FullNameChanged(value)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(text = stringResource(R.string.profile_full_name)) }
                )
            }

            OutlinedTextField(
                value = state.email,
                onValueChange = { value -> onEvent(AuthEvent.EmailChanged(value)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(text = stringResource(R.string.profile_email)) }
            )

            OutlinedTextField(
                value = state.password,
                onValueChange = { value -> onEvent(AuthEvent.PasswordChanged(value)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                label = { Text(text = stringResource(R.string.auth_password)) }
            )

            if (state.mode == AuthMode.Register) {
                RoleSelector(
                    selectedRole = state.selectedRole,
                    onRoleSelected = { role -> onEvent(AuthEvent.RoleSelected(role)) }
                )
            }

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    if (state.mode == AuthMode.Login) {
                        onEvent(AuthEvent.LoginClicked)
                    } else {
                        onEvent(AuthEvent.RegisterClicked)
                    }
                },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (state.isLoading) {
                        stringResource(R.string.processing)
                    } else if (state.mode == AuthMode.Login) {
                        stringResource(R.string.auth_login_action)
                    } else {
                        stringResource(R.string.auth_register_action)
                    }
                )
            }

            TextButton(
                onClick = {
                    if (state.mode == AuthMode.Login) {
                        onEvent(AuthEvent.SwitchToRegisterClicked)
                    } else {
                        onEvent(AuthEvent.SwitchToLoginClicked)
                    }
                },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (state.mode == AuthMode.Login) {
                        stringResource(R.string.auth_switch_to_register)
                    } else {
                        stringResource(R.string.auth_switch_to_login)
                    }
                )
            }
        }
    }
}

@Composable
private fun RoleSelector(
    selectedRole: UserRole,
    onRoleSelected: (UserRole) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Small)
    ) {
        Text(
            text = stringResource(R.string.profile_current_role),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(RhythmSpacing.Small)
        ) {
            FilterChip(
                selected = selectedRole == UserRole.PATIENT,
                onClick = { onRoleSelected(UserRole.PATIENT) },
                label = { Text(text = stringResource(R.string.role_patient)) }
            )

            FilterChip(
                selected = selectedRole == UserRole.DOCTOR,
                onClick = { onRoleSelected(UserRole.DOCTOR) },
                label = { Text(text = stringResource(R.string.role_doctor)) }
            )
        }
    }
}
