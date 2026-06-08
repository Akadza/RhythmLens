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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.domain.model.UserRole
import com.rimuru.android.rhythmlens.ui.theme.LocalRhythmThemeController
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                modifier = Modifier.height(88.dp),
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
                ThemeCard()
            }

            item {
                LogoutCard(
                    isLoggingOut = state.isLoggingOut,
                    onLogoutClick = {
                        onEvent(ProfileEvent.LogoutClicked)
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

            when (state.role) {
                UserRole.PATIENT -> {
                    ProfileInfoRow(
                        title = stringResource(R.string.profile_invite_code),
                        value = when {
                            state.isInviteCodeLoading -> stringResource(R.string.processing)
                            state.inviteCode.isNullOrBlank() -> stringResource(R.string.not_specified)
                            else -> state.inviteCode
                        }
                    )
                }

                UserRole.DOCTOR -> {
                    ProfileInfoRow(
                        title = stringResource(R.string.profile_selected_patient),
                        value = state.selectedPatientName
                            ?: state.selectedPatientId
                            ?: stringResource(R.string.not_specified)
                    )
                }

                null -> Unit
            }
        }
    }
}

@Composable
private fun ThemeCard() {
    val themeController = LocalRhythmThemeController.current ?: return
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(RhythmSpacing.ExtraSmall)
            ) {
                Text(
                    text = stringResource(R.string.profile_appearance),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.profile_dark_theme),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Switch(
                checked = themeController.isDarkTheme,
                onCheckedChange = {
                    themeController.onToggleTheme()
                }
            )
        }
    }
}

@Composable
private fun LogoutCard(
    isLoggingOut: Boolean,
    onLogoutClick: () -> Unit
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
                text = stringResource(R.string.profile_session),
                style = MaterialTheme.typography.titleMedium
            )

            Button(
                onClick = onLogoutClick,
                enabled = !isLoggingOut,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isLoggingOut) {
                        stringResource(R.string.processing)
                    } else {
                        stringResource(R.string.logout)
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    Spacer(modifier = Modifier.height(RhythmSpacing.ExtraSmall))
}
