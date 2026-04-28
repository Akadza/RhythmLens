package com.rimuru.android.rhythmlens.domain.model

import java.time.Instant

data class EcgRecord (
    val id: String,
    val patientId: String,
    val recordedAt: Instant,
    val originalImageUrl: String?,
    val digitizedSignal: DigitizedEcg?,
    val heartRate: Int?,
    val status: EcgStatus = EcgStatus.PENDING,
    val doctorId: String? = null
)

data class DigitizedEcg(
    val leads: Map<EcgLead, List<EcgPoint>>,
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

enum class EcgStatus {
    PENDING,
    PROCESSED,
    ERROR
}
