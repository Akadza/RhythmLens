package com.rimuru.android.rhythmlens.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rimuru.android.rhythmlens.data.local.converter.Converters
import com.rimuru.android.rhythmlens.data.local.dao.EcgDao
import com.rimuru.android.rhythmlens.data.local.dao.EcgSignalDao
import com.rimuru.android.rhythmlens.data.local.dao.PatientDao
import com.rimuru.android.rhythmlens.data.local.entity.EcgRecordEntity
import com.rimuru.android.rhythmlens.data.local.entity.EcgSignalLeadEntity
import com.rimuru.android.rhythmlens.data.local.entity.PatientEntity

@Database(
    entities = [
        EcgRecordEntity::class,
        EcgSignalLeadEntity::class,
        PatientEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RhythmLensDatabase : RoomDatabase() {

    abstract fun ecgDao(): EcgDao
    abstract fun ecgSignalDao(): EcgSignalDao
    abstract fun patientDao(): PatientDao
}
