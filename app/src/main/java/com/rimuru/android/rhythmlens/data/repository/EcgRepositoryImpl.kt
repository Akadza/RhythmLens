package com.rimuru.android.rhythmlens.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.rimuru.android.rhythmlens.data.local.RhythmLensDatabase
import com.rimuru.android.rhythmlens.data.local.dao.EcgDao
import com.rimuru.android.rhythmlens.data.local.dao.EcgSignalDao
import com.rimuru.android.rhythmlens.data.local.entity.EcgRecordEntity
import com.rimuru.android.rhythmlens.data.remote.api.EcgApi
import com.rimuru.android.rhythmlens.data.remote.dto.EcgPredictionDto
import com.rimuru.android.rhythmlens.data.remote.dto.EcgRecordDto
import com.rimuru.android.rhythmlens.data.remote.dto.EcgSignalDto
import com.rimuru.android.rhythmlens.data.remote.dto.EcgSignalSegmentDto
import com.rimuru.android.rhythmlens.data.repository.mapper.EcgSignalBinaryMapper
import com.rimuru.android.rhythmlens.domain.model.DigitizedEcg
import com.rimuru.android.rhythmlens.domain.model.EcgLead
import com.rimuru.android.rhythmlens.domain.model.EcgLeadOrigin
import com.rimuru.android.rhythmlens.domain.model.EcgLeadSegment
import com.rimuru.android.rhythmlens.domain.model.EcgPoint
import com.rimuru.android.rhythmlens.domain.model.EcgPrediction
import com.rimuru.android.rhythmlens.domain.model.EcgRecord
import com.rimuru.android.rhythmlens.domain.model.EcgStatus
import com.rimuru.android.rhythmlens.domain.repository.EcgRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.time.Instant
import javax.inject.Inject

class EcgRepositoryImpl @Inject constructor(
    private val database: RhythmLensDatabase,
    private val ecgDao: EcgDao,
    private val ecgSignalDao: EcgSignalDao,
    private val ecgSignalBinaryMapper: EcgSignalBinaryMapper,
    private val ecgApi: EcgApi,
    @ApplicationContext private val context: Context
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
            doctorId = doctorId,
            topPredictions = decodeTopPredictions(topPredictionsJson)
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
            reconstructedLeadCount = reconstructedLeadCount,
            topPredictionsJson = encodeTopPredictions(topPredictions)
        )
    }

    override suspend fun digitizeEcg(imageUri: String): EcgRecord {
        val uploadedRecord = uploadImageToBackend(imageUri)
        val signal = loadRemoteSignalOrNull(uploadedRecord.id, uploadedRecord.status.toEcgStatus())
        val domainRecord = uploadedRecord.toDomain(signal = signal)
        saveEcg(domainRecord)
        return domainRecord
    }

    private suspend fun uploadImageToBackend(imageUri: String): EcgRecordDto {
        return withContext(Dispatchers.IO) {
            val uri = Uri.parse(imageUri)
            val tempFile = copyUriToTempFile(uri)
            try {
                val requestBody = tempFile
                    .asRequestBody(resolveMimeType(uri).toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData(
                    name = "file",
                    filename = tempFile.name,
                    body = requestBody
                )

                ecgApi.uploadEcg(part)
            } finally {
                tempFile.delete()
            }
        }
    }

    private suspend fun loadRemoteSignalOrNull(ecgId: String, status: EcgStatus): DigitizedEcg? {
        if (status != EcgStatus.PROCESSED) {
            return null
        }

        return runCatching {
            ecgApi.getEcgSignal(ecgId).toDomain()
        }.getOrNull()
    }

    private fun copyUriToTempFile(uri: Uri): File {
        val extension = when (context.contentResolver.getType(uri)) {
            "image/jpeg" -> ".jpg"
            "image/png" -> ".png"
            else -> when (uri.path?.substringAfterLast('.', missingDelimiterValue = "")?.lowercase()) {
                "jpg", "jpeg" -> ".jpg"
                "png" -> ".png"
                else -> ".png"
            }
        }

        val tempFile = File.createTempFile("ecg_upload_", extension, context.cacheDir)
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open ECG image" }
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }

    private fun resolveMimeType(uri: Uri): String {
        return context.contentResolver.getType(uri) ?: when (uri.path?.substringAfterLast('.', missingDelimiterValue = "")?.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "image/png"
        }
    }

    override suspend fun saveEcg(record: EcgRecord) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val signal = record.digitizedSignal

                ecgDao.insert(record.toEntity())

                if (signal != null) {
                    ecgSignalDao.deleteLeadsForEcg(record.id)
                    ecgSignalDao.deleteSegmentsForEcg(record.id)
                    ecgSignalDao.insertAll(
                        ecgSignalBinaryMapper.toEntities(
                            ecgId = record.id,
                            signal = signal
                        )
                    )
                    ecgSignalDao.insertSegments(
                        ecgSignalBinaryMapper.toSegmentEntities(
                            ecgId = record.id,
                            signal = signal
                        )
                    )
                }
            }
        }
    }

    override suspend fun syncEcgFromBackend(): List<EcgRecord> {
        return withContext(Dispatchers.IO) {
            val records = ecgApi.getEcgRecords().map { dto ->
                val status = dto.status.toEcgStatus()
                val remoteSignal = loadRemoteSignalOrNull(dto.id, status)
                val cachedSignal = if (remoteSignal == null) {
                    loadCachedSignalOrNull(dto.id)
                } else {
                    null
                }
                val record = dto.toDomain(signal = remoteSignal ?: cachedSignal)
                saveEcg(record)
                record
            }

            pruneLocalRecordsMissingFromBackend(records)

            records
        }
    }

    override fun getEcgById(id: String): Flow<EcgRecord?> {
        return ecgDao.getById(id).mapLatest { entity ->
            entity?.let {
                val signal = buildSignalForRecord(it)
                it.toDomain(signal = signal)
            }
        }
    }

    override fun getAllEcgForPatient(patientId: String): Flow<List<EcgRecord>> {
        return ecgDao.getAllForPatient(patientId).mapLatest { list ->
            withContext(Dispatchers.IO) {
                list.map { entity ->
                    entity.toDomain(signal = buildSignalForRecord(entity))
                }
            }
        }
    }

    override suspend fun deleteEcg(id: String) {
        withContext(Dispatchers.IO) {
            val remoteDeleteError = runCatching {
                ecgApi.deleteEcg(id)
            }.exceptionOrNull()

            if (remoteDeleteError != null && !remoteDeleteError.isHttpNotFound()) {
                throw remoteDeleteError
            }

            database.withTransaction {
                ecgSignalDao.deleteLeadsForEcg(id)
                ecgSignalDao.deleteSegmentsForEcg(id)
                ecgDao.delete(id)
            }
        }
    }

    override suspend fun generateSyntheticImage(ecgId: String): String {
        // TODO: Запрос на сервер
        return "https://fake-server.com/synthetic/${ecgId}.png"
    }

    private suspend fun pruneLocalRecordsMissingFromBackend(records: List<EcgRecord>) {
        if (records.isEmpty()) {
            return
        }

        database.withTransaction {
            records.groupBy { record -> record.patientId }
                .forEach { (patientId, patientRecords) ->
                    val remoteIds = patientRecords.map { record -> record.id }
                    if (remoteIds.isEmpty()) {
                        ecgDao.deleteAllForPatient(patientId)
                    } else {
                        ecgDao.deleteForPatientExcept(
                            patientId = patientId,
                            ids = remoteIds
                        )
                    }
                }
        }
    }

    private suspend fun loadCachedSignalOrNull(ecgId: String): DigitizedEcg? {
        val entity = ecgDao.getByIdOnce(ecgId) ?: return null
        return buildSignalForRecord(entity)
    }

    private suspend fun buildSignalForRecord(entity: EcgRecordEntity): DigitizedEcg? {
        val samplingRate = entity.samplingRate ?: return null
        val durationSeconds = entity.durationSeconds ?: return null
        val leads = ecgSignalDao.getLeadsForEcg(entity.id)
        val segments = ecgSignalDao.getSegmentsForEcg(entity.id)

        return ecgSignalBinaryMapper.toDomain(
            leadEntities = leads,
            segmentEntities = segments,
            samplingRate = samplingRate,
            durationSeconds = durationSeconds
        )
    }

    private fun EcgRecordDto.toDomain(signal: DigitizedEcg? = null): EcgRecord {
        return EcgRecord(
            id = id,
            patientId = ownerUserId,
            recordedAt = runCatching { Instant.parse(createdAt) }.getOrDefault(Instant.now()),
            originalImageUrl = null,
            digitizedSignal = signal,
            heartRate = null,
            status = status.toEcgStatus(),
            processingMessage = null,
            errorMessage = errorMessage,
            doctorId = null,
            topPredictions = topPredictions.map { it.toDomain() }
        )
    }

    private fun EcgSignalDto.toDomain(): DigitizedEcg {
        val leadSegments = leads.mapNotNull { leadDto ->
            val lead = runCatching { EcgLead.valueOf(leadDto.lead) }.getOrNull() ?: return@mapNotNull null
            val segments = leadDto.segments.map { segmentDto ->
                segmentDto.toDomain(samplingRate)
            }
            lead to segments
        }.toMap()

        val flatLeads = leadSegments.mapValues { (_, segments) ->
            segments
                .sortedBy { segment -> segment.startSampleIndex }
                .flatMap { segment -> segment.points }
        }

        val leadOrigins = leads.mapNotNull { leadDto ->
            val lead = runCatching { EcgLead.valueOf(leadDto.lead) }.getOrNull() ?: return@mapNotNull null
            lead to leadDto.origin.toEcgLeadOrigin()
        }.toMap()

        return DigitizedEcg(
            leads = flatLeads,
            leadOrigins = leadOrigins,
            samplingRate = samplingRate,
            durationSeconds = durationSeconds,
            leadSegments = leadSegments
        )
    }

    private fun EcgSignalSegmentDto.toDomain(samplingRate: Int): EcgLeadSegment {
        return EcgLeadSegment(
            origin = origin.toEcgLeadOrigin(),
            startSampleIndex = startSampleIndex,
            points = voltage.mapIndexed { index, value ->
                val sampleIndex = startSampleIndex + index
                EcgPoint(
                    timeMs = sampleIndex * 1000L / samplingRate,
                    voltageMv = value
                )
            }
        )
    }

    private fun EcgPredictionDto.toDomain(): EcgPrediction {
        return EcgPrediction(
            label = label,
            probability = probability,
            detected = isDetected
        )
    }

    private fun encodeTopPredictions(predictions: List<EcgPrediction>): String? {
        if (predictions.isEmpty()) {
            return null
        }

        return predictionJson.encodeToString(predictions)
    }

    private fun decodeTopPredictions(raw: String?): List<EcgPrediction> {
        if (raw.isNullOrBlank()) {
            return emptyList()
        }

        return runCatching {
            predictionJson.decodeFromString<List<EcgPrediction>>(raw)
        }.getOrDefault(emptyList())
    }

    private fun String.toEcgStatus(): EcgStatus {
        return when (this) {
            "PENDING" -> EcgStatus.DIGITIZING
            else -> runCatching { EcgStatus.valueOf(this) }.getOrDefault(EcgStatus.ERROR)
        }
    }

    private fun String.toEcgLeadOrigin(): EcgLeadOrigin {
        return runCatching { EcgLeadOrigin.valueOf(this) }.getOrDefault(EcgLeadOrigin.RECONSTRUCTED)
    }

    private fun Throwable.isHttpNotFound(): Boolean {
        return this is HttpException && code() == 404
    }

    private companion object {
        val predictionJson: Json = Json {
            ignoreUnknownKeys = true
        }
    }
}
