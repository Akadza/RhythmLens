package com.rimuru.android.rhythmlens.ui.app.features.auth

import com.rimuru.android.rhythmlens.domain.model.UserRole

data class AuthUiState(
    val mode: AuthMode = AuthMode.Login,
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val selectedRole: UserRole = UserRole.PATIENT,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

enum class AuthMode {
    Login,
    Register
}

sealed interface AuthEvent {
    data object LoginClicked : AuthEvent
    data object RegisterClicked : AuthEvent
    data object SwitchToLoginClicked : AuthEvent
    data object SwitchToRegisterClicked : AuthEvent
    data class FullNameChanged(val value: String) : AuthEvent
    data class EmailChanged(val value: String) : AuthEvent
    data class PasswordChanged(val value: String) : AuthEvent
    data class RoleSelected(val role: UserRole) : AuthEvent
}
