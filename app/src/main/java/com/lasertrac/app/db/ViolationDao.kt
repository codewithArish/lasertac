package com.lasertrac.app.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ViolationDao {
    @Query("SELECT * FROM violations ORDER BY id ASC")
    fun getAllViolations(): Flow<List<ViolationEntity>>

    @Insert
    suspend fun insertViolation(violation: ViolationEntity): Long

    @Update
    suspend fun updateViolation(violation: ViolationEntity)

    @Delete
    suspend fun deleteViolation(violation: ViolationEntity)
}


