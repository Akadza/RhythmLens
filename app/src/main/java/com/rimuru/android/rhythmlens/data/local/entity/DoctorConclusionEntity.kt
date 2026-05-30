package com.rimuru.android.rhythmlens.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "doctor_conclusions",
    foreignKeys = [
        ForeignKey(
            entity = EcgRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["ecgId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["ecgId"]),
        Index(value = ["doctorId"])
    ]
)
data class DoctorConclusionEntity(
    @PrimaryKey
    val ecgId: String,
    val doctorId: String,
    val text: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
