package com.rimuru.android.rhythmlens.domain.usecase

import com.rimuru.android.rhythmlens.domain.model.UserRole
import com.rimuru.android.rhythmlens.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SwitchUserRoleUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(role: UserRole) {
        val currentUser = sessionRepository.observeCurrentUser().first() ?: return
        val selectedPatientId = when (role) {
            UserRole.PATIENT -> DEFAULT_PATIENT_ID
            UserRole.DOCTOR -> sessionRepository.observeSelectedPatientId().first() ?: DEFAULT_PATIENT_ID
        }

        sessionRepository.saveSession(
            user = currentUser.copy(role = role),
            selectedPatientId = selectedPatientId
        )
    }

    private companion object {
        const val DEFAULT_PATIENT_ID = "temp-patient-id"
    }
}
