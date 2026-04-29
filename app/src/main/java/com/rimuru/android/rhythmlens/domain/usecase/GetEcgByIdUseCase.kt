package com.rimuru.android.rhythmlens.domain.usecase

import com.rimuru.android.rhythmlens.domain.model.EcgRecord
import com.rimuru.android.rhythmlens.domain.repository.EcgRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEcgByIdUseCase @Inject constructor(
    private val ecgRepository: EcgRepository
) {
    operator fun invoke(id: String): Flow<EcgRecord?> {
        return ecgRepository.getEcgById(id)
    }
}