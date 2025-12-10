package com.lasertrac.app.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snap: SnapDetail)

    // Modified to return a Flow for reactive updates on a background thread.
    @Query("SELECT * FROM snap_details ORDER BY dateTime DESC")
    fun getAllSnaps(): Flow<List<SnapDetail>>

    @Query("SELECT * FROM snap_details WHERE id = :snapId")
    fun getSnapById(snapId: String): Flow<SnapDetail?>

    @Query("DELETE FROM snap_details WHERE id IN (:snapIds)")
    suspend fun deleteSnaps(snapIds: List<String>)
}
