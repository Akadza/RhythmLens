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
    val digitizedSignalJson: String,      // храним как JSON-строку
    val heartRate: Int?,
    val status: String,                   // "PENDING", "PROCESSED", "ERROR"
    val doctorId: String?,
    val createdAt: Instant = Instant.now()
)