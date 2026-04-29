package com.rimuru.android.rhythmlens.domain.usecase

import com.rimuru.android.rhythmlens.domain.repository.EcgRepository
import javax.inject.Inject

class GenerateSyntheticImageUseCase @Inject constructor(
    private val ecgRepository: EcgRepository
) {
    suspend operator fun invoke(id: String): String {
        return ecgRepository.generateSyntheticImage(id)
    }
}