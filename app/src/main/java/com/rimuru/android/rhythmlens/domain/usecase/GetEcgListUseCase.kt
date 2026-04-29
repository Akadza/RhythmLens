package com.rimuru.android.rhythmlens.domain.usecase

import com.rimuru.android.rhythmlens.domain.model.EcgRecord
import com.rimuru.android.rhythmlens.domain.model.Patient
import com.rimuru.android.rhythmlens.domain.repository.EcgRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEcgListUseCase @Inject constructor(
    private val ecgRepository: EcgRepository
) {
    operator fun invoke(patientId: String): Flow<List<EcgRecord?>> {
        return ecgRepository.getAllEcgForPatient(patientId)
    }
}