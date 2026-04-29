package com.rimuru.android.rhythmlens.domain.usecase

import com.rimuru.android.rhythmlens.domain.model.EcgRecord
import com.rimuru.android.rhythmlens.domain.repository.EcgRepository
import javax.inject.Inject

class DigitizeEcgUseCase @Inject constructor(
    private val ecgRepository: EcgRepository
) {
    suspend operator fun invoke(imageUri: String): EcgRecord {
        return ecgRepository.digitizeEcg(imageUri)
    }
}


