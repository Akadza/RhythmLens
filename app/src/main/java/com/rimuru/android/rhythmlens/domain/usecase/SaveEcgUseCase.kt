package com.rimuru.android.rhythmlens.domain.usecase

import com.rimuru.android.rhythmlens.domain.model.EcgRecord
import com.rimuru.android.rhythmlens.domain.repository.EcgRepository
import javax.inject.Inject

class SaveEcgUseCase @Inject constructor(
    private val ecgRepository: EcgRepository
) {
    suspend operator fun invoke(ecg: EcgRecord) {
        return ecgRepository.saveEcg(ecg)
    }
}