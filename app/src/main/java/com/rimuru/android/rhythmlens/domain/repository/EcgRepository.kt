package com.rimuru.android.rhythmlens.domain.repository

import com.rimuru.android.rhythmlens.domain.model.EcgRecord
import kotlinx.coroutines.flow.Flow

interface EcgRepository {
    suspend fun digitizeEcg(imageUri: String): EcgRecord
    suspend fun saveEcg(record: EcgRecord)
    fun getEcgById(id: String): Flow<EcgRecord?>
    fun getAllEcgForPatient(patientId: String): Flow<List<EcgRecord>>
    suspend fun deleteEcg(id: String)
    suspend fun generateSyntheticImage(ecgId: String): String
}