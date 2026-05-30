package com.rimuru.android.rhythmlens.ui.app.features.patients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimuru.android.rhythmlens.domain.model.Gender
import com.rimuru.android.rhythmlens.domain.model.Patient
import com.rimuru.android.rhythmlens.domain.model.UserRole
import com.rimuru.android.rhythmlens.domain.usecase.AttachPatientByInviteCodeUseCase
import com.rimuru.android.rhythmlens.domain.usecase.GetPatientsForDoctorUseCase
import com.rimuru.android.rhythmlens.domain.usecase.ObserveCurrentUserUseCase
import com.rimuru.android.rhythmlens.domain.usecase.ObserveSelectedPatientIdUseCase
import com.rimuru.android.rhythmlens.domain.usecase.SavePatientUseCase
import com.rimuru.android.rhythmlens.domain.usecase.SelectPatientUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PatientsViewModel @Inject constructor(
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val observeSelectedPatientIdUseCase: ObserveSelectedPatientIdUseCase,
    private val getPatientsForDoctorUseCase: GetPatientsForDoctorUseCase,
    private val savePatientUseCase: SavePatientUseCase,
    private val selectPatientUseCase: SelectPatientUseCase,
    private val attachPatientByInviteCodeUseCase: AttachPatientByInviteCodeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientsUiState(isLoading = true))
    val uiState = _uiState

    init {
        observePatients()
    }

    fun onEvent(event: PatientsEvent) {
        when (event) {
            PatientsEvent.AddTestPatientClicked -> {
                addTestPatient()
            }

            PatientsEvent.AttachPatientClicked -> {
                attachPatientByCode()
            }

            is PatientsEvent.InviteCodeChanged -> {
                _uiState.update { state ->
                    state.copy(
                        inviteCodeInput = event.value.uppercase(),
                        errorMessage = null
                    )
                }
            }

            is PatientsEvent.PatientClicked -> {
                selectPatient(event.patientId)
            }
        }
    }

    private fun observePatients() {
        viewModelScope.launch {
            combine(
                observeCurrentUserUseCase(),
                observeSelectedPatientIdUseCase()
            ) { user, selectedPatientId ->
                user to selectedPatientId
            }
                .flatMapLatest { (user, selectedPatientId) ->
                    val doctorId = user?.takeIf { it.role == UserRole.DOCTOR }?.id
                    _uiState.update { state ->
                        state.copy(
                            doctorId = doctorId,
                            selectedPatientId = selectedPatientId,
                            isLoading = true,
                            errorMessage = null
                        )
                    }

                    doctorId?.let { id ->
                        getPatientsForDoctorUseCase(id)
                    } ?: flowOf(emptyList())
                }
                .catch { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Не удалось загрузить пациентов"
                        )
                    }
                }
                .collect { patients ->
                    val selectedPatientId = _uiState.value.selectedPatientId
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            patients = patients.map { patient ->
                                patient.toUi(selectedPatientId = selectedPatientId)
                            },
                            errorMessage = null
                        )
                    }
                }
        }
    }

    private fun addTestPatient() {
        val doctorId = _uiState.value.doctorId ?: return

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isAddingPatient = true, errorMessage = null)
            }

            runCatching {
                val patient = buildTestPatient(doctorId = doctorId)
                savePatientUseCase(patient)
                selectPatientUseCase(patient.id)
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        errorMessage = throwable.message ?: "Не удалось добавить пациента"
                    )
                }
            }

            _uiState.update { state ->
                state.copy(isAddingPatient = false)
            }
        }
    }

    private fun attachPatientByCode() {
        val inviteCode = _uiState.value.inviteCodeInput.trim()
        if (inviteCode.isBlank()) {
            _uiState.update { state ->
                state.copy(errorMessage = "Введите код пациента")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isAttachingPatient = true, errorMessage = null)
            }

            attachPatientByInviteCodeUseCase(inviteCode)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(inviteCodeInput = "")
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            errorMessage = throwable.message ?: "Не удалось добавить пациента по коду"
                        )
                    }
                }

            _uiState.update { state ->
                state.copy(isAttachingPatient = false)
            }
        }
    }

    private fun selectPatient(patientId: String) {
        viewModelScope.launch {
            selectPatientUseCase(patientId)
        }
    }

    private fun buildTestPatient(doctorId: String): Patient {
        val id = UUID.randomUUID().toString()
        val shortCode = id.take(6).uppercase()

        return Patient(
            id = id,
            userId = null,
            fullName = "Иванов Иван Иванович",
            dateOfBirth = LocalDate.of(1984, 5, 12),
            gender = Gender.MALE,
            phone = "+7 900 000-00-00",
            doctorId = doctorId,
            inviteCode = "PAT-$shortCode"
        )
    }

    private fun Patient.toUi(selectedPatientId: String?): PatientItemUi {
        return PatientItemUi(
            id = id,
            fullName = fullName,
            age = dateOfBirth?.toAgeText() ?: "Возраст не указан",
            inviteCode = inviteCode,
            isSelected = id == selectedPatientId
        )
    }

    private fun LocalDate.toAgeText(): String {
        val years = Period.between(this, LocalDate.now()).years
        return "$years лет"
    }
}
