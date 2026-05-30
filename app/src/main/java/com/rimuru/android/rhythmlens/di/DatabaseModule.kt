package com.rimuru.android.rhythmlens.di

import android.content.Context
import androidx.room.Room
import com.rimuru.android.rhythmlens.data.local.RhythmLensDatabase
import com.rimuru.android.rhythmlens.data.local.dao.EcgDao
import com.rimuru.android.rhythmlens.data.local.dao.EcgSignalDao
import com.rimuru.android.rhythmlens.data.local.dao.PatientDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RhythmLensDatabase {
        return Room.databaseBuilder(
            context,
            RhythmLensDatabase::class.java,
            "rhythmlens.db"
        )
            .fallbackToDestructiveMigration()   // на время разработки
            .build()
    }

    @Provides
    fun provideEcgDao(database: RhythmLensDatabase): EcgDao {
        return database.ecgDao()
    }

    @Provides
    fun provideEcgSignalDao(database: RhythmLensDatabase): EcgSignalDao {
        return database.ecgSignalDao()
    }

    @Provides
    fun providePatientDao(database: RhythmLensDatabase): PatientDao {
        return database.patientDao()
    }
}
