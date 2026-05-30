package com.rimuru.android.rhythmlens.domain.usecase

import com.rimuru.android.rhythmlens.domain.repository.SessionRepository
import javax.inject.Inject

class SelectPatientUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(patientId: String) {
        sessionRepository.setSelectedPatientId(patientId)
    }
}
