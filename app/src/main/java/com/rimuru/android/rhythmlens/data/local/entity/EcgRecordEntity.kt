package com.rimuru.android.rhythmlens.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "ecg_records")
data class EcgRecordEntity(
    @PrimaryKey
    val id: String,
    val patientId: String,
    val recordedAt: Instant,
    val originalImageUrl: String?,
    val heartRate: Int?,
    val status: String,
    val processingMessage: String?,
    val errorMessage: String?,
    val doctorId: String?,
    val samplingRate: Int?,
    val durationSeconds: Double?,
    val digitizedLeadCount: Int,
    val reconstructedLeadCount: Int,
    val createdAt: Instant = Instant.now()
)
