package com.rimuru.android.rhythmlens.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rimuru.android.rhythmlens.data.local.entity.EcgSignalLeadEntity

@Dao
interface EcgSignalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(leads: List<EcgSignalLeadEntity>)

    @Query("SELECT * FROM ecg_signal_leads WHERE ecgId = :ecgId")
    suspend fun getLeadsForEcg(ecgId: String): List<EcgSignalLeadEntity>
}
