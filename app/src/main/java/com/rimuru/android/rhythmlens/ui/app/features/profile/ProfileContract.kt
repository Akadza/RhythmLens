package com.rimuru.android.rhythmlens.ui.app.features.profile

import com.rimuru.android.rhythmlens.domain.model.UserRole

data class ProfileUiState(
    val fullName: String = "",
    val email: String = "",
    val role: UserRole? = null,
    val selectedPatientId: String? = null,
    val isRoleChanging: Boolean = false,
    val isLoggingOut: Boolean = false
)

sealed interface ProfileEvent {
    data class RoleSelected(val role: UserRole) : ProfileEvent
    data object LogoutClicked : ProfileEvent
}
