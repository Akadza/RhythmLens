package com.rimuru.android.rhythmlens.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [],           // позже добавим entities
    version = 1,
    exportSchema = false
)
abstract class RhythmLensDatabase : RoomDatabase() {
    // DAOs будут здесь позже
}