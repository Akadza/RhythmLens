package com.rimuru.android.rhythmlens.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

data class EcgRecord(
    val id: String,
    val patientId: String,
    val recordedAt: Instant,
    val originalImageUrl: String?,
    val digitizedSignal: DigitizedEcg?,
    val heartRate: Int?,
    val status: EcgStatus = EcgStatus.DRAFT,
    val processingMessage: String? = null,
    val errorMessage: String? = null,
    val doctorId: String? = null,
    val topPredictions: List<EcgPrediction> = EcgPredictionPayloadRegistry.getPredictions(id)
)

@Serializable
data class EcgPrediction(
    val label: String,
    val probability: Double,
    val detected: Boolean
)

object EcgPredictionPayloadRegistry {
    private val json = Json { ignoreUnknownKeys = true }
    private val predictionsByEcgId = ConcurrentHashMap<String, String>()

    fun putJson(ecgId: String, predictionsJson: String?) {
        if (!predictionsJson.isNullOrBlank()) {
            predictionsByEcgId[ecgId] = predictionsJson
        }
    }

    fun putPredictions(ecgId: String, predictions: List<EcgPrediction>) {
        if (predictions.isNotEmpty()) {
            predictionsByEcgId[ecgId] = json.encodeToString(predictions)
        }
    }

    fun getJson(ecgId: String): String? {
        return predictionsByEcgId[ecgId]
    }

    fun getPredictions(ecgId: String): List<EcgPrediction> {
        val raw = predictionsByEcgId[ecgId] ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<EcgPrediction>>(raw)
        }.getOrDefault(emptyList())
    }
}

data class DigitizedEcg(
    val leads: Map<EcgLead, List<EcgPoint>>,
    val leadOrigins: Map<EcgLead, EcgLeadOrigin> = leads.keys.associateWith { EcgLeadOrigin.DIGITIZED },
    val samplingRate: Int = 500, // hz
    val durationSeconds: Double
)

data class EcgPoint(
    val timeMs: Long,
    val voltageMv: Double
)

enum class EcgLead {
    I, II, III, aVR, aVL, aVF,
    V1, V2, V3, V4, V5, V6
}

enum class EcgLeadOrigin {
    DIGITIZED,
    RECONSTRUCTED,
    MIXED
}

enum class EcgStatus {
    DRAFT,
    UPLOADING,
    DIGITIZING,
    COMPLETING,
    ANALYZING,
    PROCESSED,
    ERROR
}
