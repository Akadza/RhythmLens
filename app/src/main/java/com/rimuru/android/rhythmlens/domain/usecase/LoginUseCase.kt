package com.rimuru.android.rhythmlens.domain.usecase

import com.rimuru.android.rhythmlens.domain.model.User
import com.rimuru.android.rhythmlens.domain.model.UserRole
import com.rimuru.android.rhythmlens.domain.repository.SessionRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String
    ): Result<Unit> {
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Введите email и пароль"))
        }

        val user = User(
            id = buildLocalUserId(email),
            email = email.trim(),
            fullName = email.substringBefore('@').replaceFirstChar { char -> char.uppercase() },
            role = UserRole.PATIENT,
            createdAt = Instant.now()
        )

        sessionRepository.saveSession(
            user = user,
            selectedPatientId = DEFAULT_PATIENT_ID
        )

        return Result.success(Unit)
    }

    private fun buildLocalUserId(email: String): String {
        return "local-${email.trim().lowercase().hashCode()}-${UUID.randomUUID().toString().take(6)}"
    }

    private companion object {
        const val DEFAULT_PATIENT_ID = "temp-patient-id"
    }
}
