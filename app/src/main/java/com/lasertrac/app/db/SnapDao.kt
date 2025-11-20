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

    @Query("SELECT * FROM snap_details")
    fun getAllSnaps(): Flow<List<SnapDetail>>

    @Query("SELECT * FROM snap_details WHERE id = :snapId")
    fun getSnapById(snapId: String): Flow<SnapDetail?>

    @Query("DELETE FROM snap_details WHERE id IN (:snapIds)")
    suspend fun deleteSnaps(snapIds: List<String>)
}
