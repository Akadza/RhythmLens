package com.rimuru.android.rhythmlens.data.repository

import com.rimuru.android.rhythmlens.data.local.dao.EcgDao
import com.rimuru.android.rhythmlens.data.local.entity.EcgRecordEntity
import com.rimuru.android.rhythmlens.domain.model.DigitizedEcg
import com.rimuru.android.rhythmlens.domain.model.EcgLead
import com.rimuru.android.rhythmlens.domain.model.EcgLeadOrigin
import com.rimuru.android.rhythmlens.domain.model.EcgPoint
import com.rimuru.android.rhythmlens.domain.model.EcgRecord
import com.rimuru.android.rhythmlens.domain.model.EcgStatus
import com.rimuru.android.rhythmlens.domain.repository.EcgRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class EcgRepositoryImpl @Inject constructor(
    private val ecgDao: EcgDao
) : EcgRepository {

    private fun EcgRecordEntity.toDomain(): EcgRecord {
        return EcgRecord(
            id = id,
            patientId = patientId,
            recordedAt = recordedAt,
            originalImageUrl = originalImageUrl,
            digitizedSignal = digitizedSignalJson.toDigitizedEcgOrNull(),
            heartRate = heartRate,
            status = status.toEcgStatus(),
            processingMessage = processingMessage,
            errorMessage = errorMessage,
            doctorId = doctorId
        )
    }

    private fun EcgRecord.toEntity(): EcgRecordEntity {
        return EcgRecordEntity(
            id = id,
            patientId = patientId,
            recordedAt = recordedAt,
            originalImageUrl = originalImageUrl,
            digitizedSignalJson = digitizedSignal.toJson(),
            heartRate = heartRate,
            status = status.name,
            processingMessage = processingMessage,
            errorMessage = errorMessage,
            doctorId = doctorId
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

    private fun DigitizedEcg?.toJson(): String {
        return this?.let { ecg ->
            JSON.encodeToString(ecg.toDto())
        } ?: EMPTY_DIGITIZED_ECG_JSON
    }

    private fun String.toDigitizedEcgOrNull(): DigitizedEcg? {
        if (isBlank() || this == EMPTY_DIGITIZED_ECG_JSON) {
            return null
        }

        return runCatching {
            JSON.decodeFromString<DigitizedEcgDto>(this).toDomain()
        }.getOrNull()
    }

    private fun DigitizedEcg.toDto(): DigitizedEcgDto {
        return DigitizedEcgDto(
            samplingRate = samplingRate,
            durationSeconds = durationSeconds,
            leads = leads.mapKeys { (lead, _) -> lead.name }.mapValues { (_, points) ->
                points.map { point -> point.toDto() }
            },
            leadOrigins = leadOrigins.mapKeys { (lead, _) -> lead.name }.mapValues { (_, origin) ->
                origin.name
            }
        )
    }

    private fun DigitizedEcgDto.toDomain(): DigitizedEcg {
        val domainLeads = leads.mapKeys { (leadName, _) ->
            EcgLead.valueOf(leadName)
        }.mapValues { (_, points) ->
            points.map { point -> point.toDomain() }
        }

        val domainOrigins = leadOrigins.mapKeys { (leadName, _) ->
            EcgLead.valueOf(leadName)
        }.mapValues { (_, originName) ->
            runCatching { EcgLeadOrigin.valueOf(originName) }.getOrDefault(EcgLeadOrigin.DIGITIZED)
        }

        return DigitizedEcg(
            leads = domainLeads,
            leadOrigins = domainLeads.keys.associateWith { lead ->
                domainOrigins[lead] ?: EcgLeadOrigin.DIGITIZED
            },
            samplingRate = samplingRate,
            durationSeconds = durationSeconds
        )
    }

    private fun EcgPoint.toDto(): EcgPointDto {
        return EcgPointDto(
            timeMs = timeMs,
            voltageMv = voltageMv
        )
    }

    private fun EcgPointDto.toDomain(): EcgPoint {
        return EcgPoint(
            timeMs = timeMs,
            voltageMv = voltageMv
        )
    }

    private fun String.toEcgStatus(): EcgStatus {
        return when (this) {
            "PENDING" -> EcgStatus.DIGITIZING
            else -> runCatching { EcgStatus.valueOf(this) }.getOrDefault(EcgStatus.ERROR)
        }
    }

    private companion object {
        const val EMPTY_DIGITIZED_ECG_JSON = "{}"
        val JSON = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}

@Serializable
private data class DigitizedEcgDto(
    val samplingRate: Int,
    val durationSeconds: Double,
    val leads: Map<String, List<EcgPointDto>>,
    val leadOrigins: Map<String, String> = emptyMap()
)

@Serializable
private data class EcgPointDto(
    val timeMs: Long,
    val voltageMv: Double
)
