package com.rimuru.android.rhythmlens.data.repository

import com.rimuru.android.rhythmlens.data.local.dao.EcgDao
import com.rimuru.android.rhythmlens.data.local.dao.EcgSignalDao
import com.rimuru.android.rhythmlens.data.local.entity.EcgRecordEntity
import com.rimuru.android.rhythmlens.data.repository.mapper.EcgSignalBinaryMapper
import com.rimuru.android.rhythmlens.domain.model.DigitizedEcg
import com.rimuru.android.rhythmlens.domain.model.EcgLeadOrigin
import com.rimuru.android.rhythmlens.domain.model.EcgRecord
import com.rimuru.android.rhythmlens.domain.model.EcgStatus
import com.rimuru.android.rhythmlens.domain.repository.EcgRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class EcgRepositoryImpl @Inject constructor(
    private val ecgDao: EcgDao,
    private val ecgSignalDao: EcgSignalDao,
    private val ecgSignalBinaryMapper: EcgSignalBinaryMapper
) : EcgRepository {

    private fun EcgRecordEntity.toDomain(signal: DigitizedEcg? = null): EcgRecord {
        return EcgRecord(
            id = id,
            patientId = patientId,
            recordedAt = recordedAt,
            originalImageUrl = originalImageUrl,
            digitizedSignal = signal,
            heartRate = heartRate,
            status = status.toEcgStatus(),
            processingMessage = processingMessage,
            errorMessage = errorMessage,
            doctorId = doctorId
        )
    }

    private fun EcgRecord.toEntity(): EcgRecordEntity {
        val signal = digitizedSignal
        val digitizedLeadCount = signal?.leadOrigins
            ?.count { (_, origin) -> origin == EcgLeadOrigin.DIGITIZED || origin == EcgLeadOrigin.MIXED }
            ?: 0
        val reconstructedLeadCount = signal?.leadOrigins
            ?.count { (_, origin) -> origin == EcgLeadOrigin.RECONSTRUCTED || origin == EcgLeadOrigin.MIXED }
            ?: 0

        return EcgRecordEntity(
            id = id,
            patientId = patientId,
            recordedAt = recordedAt,
            originalImageUrl = originalImageUrl,
            heartRate = heartRate,
            status = status.name,
            processingMessage = processingMessage,
            errorMessage = errorMessage,
            doctorId = doctorId,
            samplingRate = signal?.samplingRate,
            durationSeconds = signal?.durationSeconds,
            digitizedLeadCount = digitizedLeadCount,
            reconstructedLeadCount = reconstructedLeadCount
        )
    }

    override suspend fun digitizeEcg(imageUri: String): EcgRecord {
        val fakeRecord = EcgRecord(
            id = java.util.UUID.randomUUID().toString(),
            patientId = "temp-patient-id",
            recordedAt = java.time.Instant.now(),
            originalImageUrl = imageUri,
            digitizedSignal = null,
            heartRate = 72,
            status = EcgStatus.DIGITIZING,
            processingMessage = "Оцифровка ЭКГ"
        )
        saveEcg(fakeRecord)
        return fakeRecord
    }

    override suspend fun saveEcg(record: EcgRecord) {
        ecgDao.insert(record.toEntity())

        val signal = record.digitizedSignal ?: return
        ecgSignalDao.insertAll(
            ecgSignalBinaryMapper.toEntities(
                ecgId = record.id,
                signal = signal
            )
        )
    }

    override fun getEcgById(id: String): Flow<EcgRecord?> {
        return ecgDao.getById(id).map { entity ->
            entity?.let {
                val signal = buildSignalForRecord(it)
                it.toDomain(signal = signal)
            }
        }
    }

    override fun getAllEcgForPatient(patientId: String): Flow<List<EcgRecord>> {
        return ecgDao.getAllForPatient(patientId).map { list ->
            list.map { it.toDomain(signal = null) }
        }
    }

    override suspend fun deleteEcg(id: String) {
        ecgDao.delete(id)
    }

    override suspend fun generateSyntheticImage(ecgId: String): String {
        // TODO: Запрос на сервер
        return "https://fake-server.com/synthetic/${ecgId}.png"
    }

    private suspend fun buildSignalForRecord(entity: EcgRecordEntity): DigitizedEcg? {
        val samplingRate = entity.samplingRate ?: return null
        val durationSeconds = entity.durationSeconds ?: return null
        val leads = ecgSignalDao.getLeadsForEcg(entity.id)

        return ecgSignalBinaryMapper.toDomain(
            leadEntities = leads,
            samplingRate = samplingRate,
            durationSeconds = durationSeconds
        )
    }

    private fun String.toEcgStatus(): EcgStatus {
        return when (this) {
            "PENDING" -> EcgStatus.DIGITIZING
            else -> runCatching { EcgStatus.valueOf(this) }.getOrDefault(EcgStatus.ERROR)
        }
    }
}
