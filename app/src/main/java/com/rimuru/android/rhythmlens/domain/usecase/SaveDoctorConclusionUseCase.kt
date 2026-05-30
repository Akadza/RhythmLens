package com.rimuru.android.rhythmlens.domain.usecase

import com.rimuru.android.rhythmlens.domain.model.DoctorConclusion
import com.rimuru.android.rhythmlens.domain.repository.DoctorConclusionRepository
import com.rimuru.android.rhythmlens.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

class SaveDoctorConclusionUseCase @Inject constructor(
    private val doctorConclusionRepository: DoctorConclusionRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(
        ecgId: String,
        text: String,
        existingConclusion: DoctorConclusion?
    ) {
        val doctor = sessionRepository.observeCurrentUser().first()
            ?: throw IllegalStateException("Текущий пользователь не найден")
        val now = Instant.now()

        doctorConclusionRepository.saveConclusion(
            DoctorConclusion(
                ecgId = ecgId,
                doctorId = doctor.id,
                text = text.trim(),
                createdAt = existingConclusion?.createdAt ?: now,
                updatedAt = now
            )
        )
    }
}
