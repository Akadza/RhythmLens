package com.rimuru.android.rhythmlens.data.repository.dto

import kotlinx.serialization.Serializable

@Serializable
data class DigitizedEcgDto(
    val samplingRate: Int,
    val durationSeconds: Double,
    val leads: Map<String, List<EcgPointDto>>,
    val leadOrigins: Map<String, String> = emptyMap()
)

@Serializable
data class EcgPointDto(
    val timeMs: Long,
    val voltageMv: Double
)
