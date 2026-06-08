package com.rimuru.android.rhythmlens.ui.app.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimuru.android.rhythmlens.domain.usecase.LoginUseCase
import com.rimuru.android.rhythmlens.domain.usecase.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: AuthEvent) {
        when (event) {
            AuthEvent.LoginClicked -> submitLogin()
            AuthEvent.RegisterClicked -> submitRegister()
            AuthEvent.SwitchToLoginClicked -> _uiState.update { it.copy(mode = AuthMode.Login, errorMessage = null) }
            AuthEvent.SwitchToRegisterClicked -> _uiState.update { it.copy(mode = AuthMode.Register, errorMessage = null) }
            is AuthEvent.FullNameChanged -> _uiState.update {
                it.copy(
                    fullName = event.value.take(MAX_FULL_NAME_LENGTH),
                    errorMessage = null
                )
            }
            is AuthEvent.EmailChanged -> _uiState.update {
                it.copy(
                    email = event.value.trim().take(MAX_EMAIL_LENGTH),
                    errorMessage = null
                )
            }
            is AuthEvent.PasswordChanged -> _uiState.update { it.copy(password = event.value, errorMessage = null) }
            is AuthEvent.RoleSelected -> _uiState.update { it.copy(selectedRole = event.role, errorMessage = null) }
        }
    }

    private fun submitLogin() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            loginUseCase(state.email, state.password).onFailure { throwable ->
                _uiState.update { it.copy(errorMessage = throwable.message ?: "Не удалось войти") }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun submitRegister() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            registerUseCase(
                state.fullName.trim(),
                state.email.trim(),
                state.password,
                state.selectedRole
            ).onFailure { throwable ->
                _uiState.update { it.copy(errorMessage = throwable.message ?: "Не удалось зарегистрироваться") }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private companion object {
        const val MAX_FULL_NAME_LENGTH = 120
        const val MAX_EMAIL_LENGTH = 254
    }
}
