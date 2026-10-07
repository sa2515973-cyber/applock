package com.example.applock.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LockedAppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: LockedApp)

    @Delete
    suspend fun delete(app: LockedApp)

    @Query("SELECT * FROM locked_apps")
    fun observeAll(): Flow<List<LockedApp>>

    @Query("SELECT packageName FROM locked_apps")
    suspend fun allPackageNamesOnce(): List<String>
}
