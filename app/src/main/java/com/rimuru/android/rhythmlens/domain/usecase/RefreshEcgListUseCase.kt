package com.rimuru.android.rhythmlens.domain.usecase

import com.rimuru.android.rhythmlens.domain.model.EcgRecord
import com.rimuru.android.rhythmlens.domain.repository.EcgRepository
import javax.inject.Inject

class RefreshEcgListUseCase @Inject constructor(
    private val ecgRepository: EcgRepository
) {
    suspend operator fun invoke(): List<EcgRecord> {
        return ecgRepository.syncEcgFromBackend()
    }
}