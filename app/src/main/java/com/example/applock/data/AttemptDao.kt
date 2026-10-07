package com.example.applock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttemptDao {
    @Insert
    suspend fun insert(attempt: Attempt)

    @Query("SELECT * FROM attempts ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<Attempt>>

    @Query("DELETE FROM attempts")
    suspend fun clearAll()

    @Query("DELETE FROM attempts WHERE id = :id")
    suspend fun delete(id: Long)
}
