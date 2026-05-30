package com.rimuru.android.rhythmlens.ui.app.features.patients

data class PatientsUiState(
    val doctorId: String? = null,
    val selectedPatientId: String? = null,
    val patients: List<PatientItemUi> = emptyList(),
    val isLoading: Boolean = false,
    val isAddingPatient: Boolean = false,
    val errorMessage: String? = null
)

data class PatientItemUi(
    val id: String,
    val fullName: String,
    val age: String,
    val inviteCode: String,
    val isSelected: Boolean
)

sealed interface PatientsEvent {
    data object AddTestPatientClicked : PatientsEvent
    data class PatientClicked(val patientId: String) : PatientsEvent
}
