package com.rimuru.android.rhythmlens.domain.usecase

import com.rimuru.android.rhythmlens.domain.model.UserRole
import com.rimuru.android.rhythmlens.domain.repository.AuthRepository
import com.rimuru.android.rhythmlens.domain.repository.SessionRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(
        email: String,
        credential: String
    ): Result<Unit> {
        return authRepository.signIn(
            email = email,
            credential = credential
        ).map { user ->
            val selectedPatientId = if (user.role == UserRole.PATIENT) {
                user.id
            } else {
                null
            }

            sessionRepository.saveSession(
                user = user,
                selectedPatientId = selectedPatientId
            )
        }
    }
}
