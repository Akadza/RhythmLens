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
        val selectedPatientId = sessionRepository.observeSelectedPatientId().first()

        sessionRepository.saveSession(
            user = currentUser.copy(role = role),
            selectedPatientId = selectedPatientId ?: DEFAULT_PATIENT_ID
        )
    }

    private companion object {
        const val DEFAULT_PATIENT_ID = "temp-patient-id"
    }
}
