package com.rimuru.android.rhythmlens.domain.usecase

import com.rimuru.android.rhythmlens.domain.model.EcgRecord
import com.rimuru.android.rhythmlens.domain.repository.EcgRepository
import javax.inject.Inject

class CompareEcgUseCase @Inject constructor(    ) {
    suspend operator fun invoke(ecg1: EcgRecord, ecg2: EcgRecord): ComparisonResult {
        /**
         * TODO: Implement comparison logic between two ECG records
         */
        return ComparisonResult(
            commonFeatures = listOf(),
            differences = listOf(),
            ecg1 = ecg1,
            ecg2 = ecg2
        )
    }
}

data class ComparisonResult(
    val ecg1: EcgRecord,
    val ecg2: EcgRecord,
    val commonFeatures: List<String>,
    val differences: List<String>
)