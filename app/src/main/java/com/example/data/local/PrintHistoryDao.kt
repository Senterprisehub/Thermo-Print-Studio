package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PrintHistoryDao {
    @Query("SELECT * FROM print_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<PrintHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PrintHistoryEntity): Long

    @Query("DELETE FROM print_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM print_history")
    suspend fun clearAll()
}
