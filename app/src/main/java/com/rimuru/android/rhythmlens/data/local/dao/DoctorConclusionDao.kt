package com.rimuru.android.rhythmlens.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rimuru.android.rhythmlens.data.local.entity.DoctorConclusionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DoctorConclusionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conclusion: DoctorConclusionEntity)

    @Query("SELECT * FROM doctor_conclusions WHERE ecgId = :ecgId LIMIT 1")
    fun observeByEcgId(ecgId: String): Flow<DoctorConclusionEntity?>
}
