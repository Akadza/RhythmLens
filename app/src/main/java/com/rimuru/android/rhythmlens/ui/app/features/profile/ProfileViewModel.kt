package com.rimuru.android.rhythmlens.ui.app.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimuru.android.rhythmlens.domain.usecase.ObserveCurrentUserUseCase
import com.rimuru.android.rhythmlens.domain.usecase.ObserveSelectedPatientIdUseCase
import com.rimuru.android.rhythmlens.domain.usecase.SwitchUserRoleUseCase
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
    private val switchUserRoleUseCase: SwitchUserRoleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeProfile()
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.RoleSelected -> {
                switchRole(event.role)
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
                    isRoleChanging = _uiState.value.isRoleChanging
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun switchRole(role: com.rimuru.android.rhythmlens.domain.model.UserRole) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isRoleChanging = true)
            }

            runCatching {
                switchUserRoleUseCase(role)
            }

            _uiState.update { state ->
                state.copy(isRoleChanging = false)
            }
        }
    }
}
