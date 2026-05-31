package com.rimuru.android.rhythmlens.domain.usecase

import com.rimuru.android.rhythmlens.domain.repository.SessionRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke() {
        sessionRepository.clearSession()
    }
}
