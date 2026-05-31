package com.rimuru.android.rhythmlens.ui.app.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimuru.android.rhythmlens.domain.usecase.LogoutUseCase
import com.rimuru.android.rhythmlens.domain.usecase.ObserveCurrentUserUseCase
import com.rimuru.android.rhythmlens.domain.usecase.ObserveSelectedPatientIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val observeSelectedPatientIdUseCase: ObserveSelectedPatientIdUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeProfile()
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            ProfileEvent.LogoutClicked -> {
                logout()
            }
        }
    }

    private fun observeProfile() {
        viewModelScope.launch {
            combine(
                observeCurrentUserUseCase(),
                observeSelectedPatientIdUseCase()
            ) { user, selectedPatientId ->
                ProfileUiState(
                    fullName = user?.fullName.orEmpty(),
                    email = user?.email.orEmpty(),
                    role = user?.role,
                    selectedPatientId = selectedPatientId,
                    isLoggingOut = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isLoggingOut = true)
            }

            runCatching {
                logoutUseCase()
            }.onFailure {
                _uiState.update { state ->
                    state.copy(isLoggingOut = false)
                }
            }
        }
    }
}
