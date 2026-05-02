package com.rimuru.android.rhythmlens.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rimuru.android.rhythmlens.data.local.converter.Converters
import com.rimuru.android.rhythmlens.data.local.dao.EcgDao
import com.rimuru.android.rhythmlens.data.local.entity.EcgRecordEntity
import com.rimuru.android.rhythmlens.data.local.entity.PatientEntity

@Database(
    entities = [
        EcgRecordEntity::class,
        PatientEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RhythmLensDatabase : RoomDatabase() {

    abstract fun ecgDao(): EcgDao
    // abstract fun patientDao(): PatientDao  ← позже
}