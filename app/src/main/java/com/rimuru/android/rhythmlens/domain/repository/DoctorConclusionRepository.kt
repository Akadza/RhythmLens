package com.rimuru.android.rhythmlens.domain.repository

import com.rimuru.android.rhythmlens.domain.model.DoctorConclusion
import kotlinx.coroutines.flow.Flow

interface DoctorConclusionRepository {

    fun observeByEcgId(ecgId: String): Flow<DoctorConclusion?>

    suspend fun saveConclusion(conclusion: DoctorConclusion)
}
