package com.rimuru.android.rhythmlens.data.repository

import com.rimuru.android.rhythmlens.data.local.dao.DoctorConclusionDao
import com.rimuru.android.rhythmlens.data.local.entity.DoctorConclusionEntity
import com.rimuru.android.rhythmlens.data.remote.api.EcgApi
import com.rimuru.android.rhythmlens.data.remote.dto.DoctorConclusionDto
import com.rimuru.android.rhythmlens.data.remote.dto.SaveDoctorConclusionRequestDto
import com.rimuru.android.rhythmlens.domain.model.DoctorConclusion
import com.rimuru.android.rhythmlens.domain.repository.DoctorConclusionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.time.Instant
import javax.inject.Inject

class DoctorConclusionRepositoryImpl @Inject constructor(
    private val doctorConclusionDao: DoctorConclusionDao,
    private val ecgApi: EcgApi
) : DoctorConclusionRepository {

    override fun observeByEcgId(ecgId: String): Flow<DoctorConclusion?> {
        return doctorConclusionDao.observeByEcgId(ecgId)
            .onStart {
                refreshFromBackend(ecgId)
            }
            .map { entity ->
                entity?.toDomain()
            }
    }

    override suspend fun saveConclusion(conclusion: DoctorConclusion) {
        val saved = ecgApi.saveDoctorConclusion(
            ecgId = conclusion.ecgId,
            request = SaveDoctorConclusionRequestDto(text = conclusion.text)
        ).toDomain()

        doctorConclusionDao.insert(saved.toEntity())
    }

    private suspend fun refreshFromBackend(ecgId: String) {
        runCatching {
            ecgApi.getDoctorConclusion(ecgId)
        }.onSuccess { dto ->
            if (dto == null) {
                doctorConclusionDao.deleteByEcgId(ecgId)
            } else {
                doctorConclusionDao.insert(dto.toDomain().toEntity())
            }
        }
    }

    private fun DoctorConclusionDto.toDomain(): DoctorConclusion {
        return DoctorConclusion(
            ecgId = ecgId,
            doctorId = doctorId,
            text = text,
            createdAt = parseInstantOrNow(createdAt),
            updatedAt = parseInstantOrNow(updatedAt)
        )
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

    private fun parseInstantOrNow(value: String): Instant {
        return runCatching {
            Instant.parse(value)
        }.getOrDefault(Instant.now())
    }
}