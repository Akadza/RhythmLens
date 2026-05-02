package com.rimuru.android.rhythmlens.data.repository

import com.rimuru.android.rhythmlens.data.local.dao.EcgDao
import com.rimuru.android.rhythmlens.data.local.entity.EcgRecordEntity
import com.rimuru.android.rhythmlens.domain.model.EcgRecord
import com.rimuru.android.rhythmlens.domain.model.EcgStatus
import com.rimuru.android.rhythmlens.domain.repository.EcgRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class EcgRepositoryImpl @Inject constructor(
    private val ecgDao: EcgDao
) : EcgRepository {

    // ─────────────────────────────────────────────────────────────
    // Мапперы Entity ↔ Domain
    // ─────────────────────────────────────────────────────────────
    private fun EcgRecordEntity.toDomain(): EcgRecord {
        return EcgRecord(
            id = id,
            patientId = patientId,
            recordedAt = recordedAt,
            originalImageUrl = originalImageUrl,
            digitizedSignal = null,           // TODO: parse JSON
            heartRate = heartRate,
            status = EcgStatus.valueOf(status),
            doctorId = doctorId
        )
    }

    private fun EcgRecord.toEntity(): EcgRecordEntity {
        return EcgRecordEntity(
            id = id,
            patientId = patientId,
            recordedAt = recordedAt,
            originalImageUrl = originalImageUrl,
            digitizedSignalJson = "{}",       // TODO: parse JSON
            heartRate = heartRate,
            status = status.name,
            doctorId = doctorId
        )
    }

    // ─────────────────────────────────────────────────────────────
    // Реализация интерфейса
    // ─────────────────────────────────────────────────────────────
    override suspend fun digitizeEcg(imageUri: String): EcgRecord {
        // TODO: Отправка фото на сервер + получение оцифрованного сигнала
        // Пока возвращаем заглушку
        val fakeRecord = EcgRecord(
            id = java.util.UUID.randomUUID().toString(),
            patientId = "temp-patient-id",
            recordedAt = java.time.Instant.now(),
            originalImageUrl = imageUri,
            digitizedSignal = null,
            heartRate = 72,
            status = EcgStatus.PENDING
        )
        saveEcg(fakeRecord)          // сразу сохраняем в локальную БД
        return fakeRecord
    }

    override suspend fun saveEcg(record: EcgRecord) {
        ecgDao.insert(record.toEntity())
    }

    override fun getEcgById(id: String): Flow<EcgRecord?> {
        return ecgDao.getById(id).map { it?.toDomain() }
    }

    override fun getAllEcgForPatient(patientId: String): Flow<List<EcgRecord>> {
        return ecgDao.getAllForPatient(patientId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun deleteEcg(id: String) {
        ecgDao.delete(id)
    }

    override suspend fun generateSyntheticImage(ecgId: String): String {
        // TODO: Запрос на сервер
        return "https://fake-server.com/synthetic/${ecgId}.png"
    }
}