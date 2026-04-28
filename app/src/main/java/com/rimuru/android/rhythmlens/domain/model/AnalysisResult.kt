package com.rimuru.android.rhythmlens.domain.model

import java.time.Instant

// Represents the result of an ECG analysis
data class AnalysisResult (
    val id: String,
    val ecgId: String,
    val patientId: String,
    val heartRate: Int?,
    val abnormalities: List<String>,
    val aiConfidence: Float,
    val comment: String? = null,
    val createdAt: Instant = Instant.now()
)
