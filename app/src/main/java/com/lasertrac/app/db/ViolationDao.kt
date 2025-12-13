package com.lasertrac.app.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ViolationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(violation: Violation)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(violations: List<Violation>)

    @Update
    suspend fun update(violation: Violation)

    @Query("SELECT * FROM violations")
    fun getAllViolations(): Flow<List<Violation>>

    @Query("DELETE FROM violations WHERE id = :violationId")
    suspend fun deleteViolation(violationId: Int)
}
