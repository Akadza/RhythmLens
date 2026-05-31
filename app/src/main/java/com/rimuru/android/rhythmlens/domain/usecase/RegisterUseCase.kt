package com.rimuru.android.rhythmlens.domain.usecase

import com.rimuru.android.rhythmlens.domain.model.User
import com.rimuru.android.rhythmlens.domain.model.UserRole
import com.rimuru.android.rhythmlens.domain.repository.PatientRepository
import com.rimuru.android.rhythmlens.domain.repository.SessionRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val patientRepository: PatientRepository
) {
    suspend operator fun invoke(
        fullName: String,
        email: String,
        password: String,
        role: UserRole
    ): Result<Unit> {
        if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Заполните ФИО, email и пароль"))
        }

        val user = User(
            id = UUID.randomUUID().toString(),
            email = email.trim(),
            fullName = fullName.trim(),
            role = role,
            createdAt = Instant.now()
        )
        val selectedPatientId = if (role == UserRole.PATIENT) {
            user.id
        } else {
            null
        }

        if (role == UserRole.PATIENT) {
            patientRepository.savePatient(
                com.rimuru.android.rhythmlens.domain.model.Patient(
                    id = user.id,
                    userId = user.id,
                    fullName = user.fullName,
                    dateOfBirth = null,
                    gender = null,
                    phone = null,
                    doctorId = null,
                    inviteCode = buildInviteCode()
                )
            )
        }

        sessionRepository.saveSession(
            user = user,
            selectedPatientId = selectedPatientId
        )

        return Result.success(Unit)
    }

    private fun buildInviteCode(): String {
        return "PAT-${UUID.randomUUID().toString().take(6).uppercase()}"
    }
}
