package com.rimuru.android.rhythmlens.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "ecg_signal_segments",
    primaryKeys = ["ecgId", "lead", "segmentIndex"],
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
        Index(value = ["ecgId", "lead"])
    ]
)
data class EcgSignalSegmentEntity(
    val ecgId: String,
    val lead: String,
    val segmentIndex: Int,
    val origin: String,
    val startSampleIndex: Int,
    val voltageSamples: ByteArray,
    val sampleCount: Int
)
