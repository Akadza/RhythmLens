package com.rimuru.android.rhythmlens.data.repository

import com.rimuru.android.rhythmlens.data.local.dao.DoctorConclusionDao
import com.rimuru.android.rhythmlens.data.local.entity.DoctorConclusionEntity
import com.rimuru.android.rhythmlens.domain.model.DoctorConclusion
import com.rimuru.android.rhythmlens.domain.repository.DoctorConclusionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DoctorConclusionRepositoryImpl @Inject constructor(
    private val doctorConclusionDao: DoctorConclusionDao
) : DoctorConclusionRepository {

    override fun observeByEcgId(ecgId: String): Flow<DoctorConclusion?> {
        return doctorConclusionDao.observeByEcgId(ecgId).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun saveConclusion(conclusion: DoctorConclusion) {
        doctorConclusionDao.insert(conclusion.toEntity())
    }

    private fun DoctorConclusionEntity.toDomain(): DoctorConclusion {
        return DoctorConclusion(
            ecgId = ecgId,
            doctorId = doctorId,
            text = text,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun DoctorConclusion.toEntity(): DoctorConclusionEntity {
        return DoctorConclusionEntity(
            ecgId = ecgId,
            doctorId = doctorId,
            text = text,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
