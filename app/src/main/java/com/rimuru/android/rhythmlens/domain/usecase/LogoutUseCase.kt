package com.rimuru.android.rhythmlens.domain.usecase

import com.rimuru.android.rhythmlens.domain.repository.AuthRepository
import com.rimuru.android.rhythmlens.domain.repository.LocalCacheRepository
import com.rimuru.android.rhythmlens.domain.repository.SessionRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val localCacheRepository: LocalCacheRepository
) {
    suspend operator fun invoke() {
        authRepository.signOut()
        localCacheRepository.clearAllLocalData()
        sessionRepository.clearSession()
    }
}
