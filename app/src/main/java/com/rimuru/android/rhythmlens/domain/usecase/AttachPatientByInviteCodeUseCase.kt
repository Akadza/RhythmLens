package com.rimuru.android.rhythmlens.domain.usecase

import com.rimuru.android.rhythmlens.domain.model.UserRole
import com.rimuru.android.rhythmlens.domain.repository.PatientRepository
import com.rimuru.android.rhythmlens.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AttachPatientByInviteCodeUseCase @Inject constructor(
    private val patientRepository: PatientRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(inviteCode: String): Result<String> {
        val doctor = sessionRepository.observeCurrentUser().first()
            ?: return Result.failure(IllegalStateException("Пользователь не найден"))

        if (doctor.role != UserRole.DOCTOR) {
            return Result.failure(IllegalStateException("Действие доступно только врачу"))
        }

        val patient = patientRepository.attachPatientByInviteCode(
            inviteCode = inviteCode,
            doctorId = doctor.id
        )

        sessionRepository.setSelectedPatientId(patient.id)

        return Result.success(patient.id)
    }
}