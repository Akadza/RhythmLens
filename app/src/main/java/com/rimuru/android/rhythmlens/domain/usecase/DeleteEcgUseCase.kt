package com.rimuru.android.rhythmlens.domain.usecase

import com.rimuru.android.rhythmlens.domain.repository.EcgRepository
import javax.inject.Inject

class DeleteEcgUseCase @Inject constructor(
    private val ecgRepository: EcgRepository
) {
    suspend operator fun invoke(id: String) {
        return ecgRepository.deleteEcg(id)
    }
}