package com.rimuru.android.rhythmlens.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rimuru.android.rhythmlens.data.local.converter.Converters
import com.rimuru.android.rhythmlens.data.local.dao.DoctorConclusionDao
import com.rimuru.android.rhythmlens.data.local.dao.EcgDao
import com.rimuru.android.rhythmlens.data.local.dao.EcgSignalDao
import com.rimuru.android.rhythmlens.data.local.dao.PatientDao
import com.rimuru.android.rhythmlens.data.local.dao.UserDao
import com.rimuru.android.rhythmlens.data.local.entity.DoctorConclusionEntity
import com.rimuru.android.rhythmlens.data.local.entity.EcgRecordEntity
import com.rimuru.android.rhythmlens.data.local.entity.EcgSignalLeadEntity
import com.rimuru.android.rhythmlens.data.local.entity.EcgSignalSegmentEntity
import com.rimuru.android.rhythmlens.data.local.entity.PatientEntity
import com.rimuru.android.rhythmlens.data.local.entity.UserEntity

@Database(
    entities = [
        EcgRecordEntity::class,
        EcgSignalLeadEntity::class,
        EcgSignalSegmentEntity::class,
        PatientEntity::class,
        DoctorConclusionEntity::class,
        UserEntity::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RhythmLensDatabase : RoomDatabase() {

    abstract fun ecgDao(): EcgDao
    abstract fun ecgSignalDao(): EcgSignalDao
    abstract fun patientDao(): PatientDao
    abstract fun doctorConclusionDao(): DoctorConclusionDao
    abstract fun userDao(): UserDao
}
