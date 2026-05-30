package com.rimuru.android.rhythmlens.domain.usecase

import com.rimuru.android.rhythmlens.domain.model.DoctorConclusion
import com.rimuru.android.rhythmlens.domain.repository.DoctorConclusionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDoctorConclusionUseCase @Inject constructor(
    private val doctorConclusionRepository: DoctorConclusionRepository
) {
    operator fun invoke(ecgId: String): Flow<DoctorConclusion?> {
        return doctorConclusionRepository.observeByEcgId(ecgId)
    }
}
