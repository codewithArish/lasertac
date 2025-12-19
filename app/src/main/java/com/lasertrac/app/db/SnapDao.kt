package com.lasertrac.app.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(snap: SnapDetail)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(snaps: List<SnapDetail>): List<Long>

    @Query("SELECT * FROM snap_details ORDER BY dateTime DESC")
    fun getAllSnaps(): Flow<List<SnapDetail>>

    @Query("SELECT * FROM snap_details WHERE id = :snapId")
    fun getSnapById(snapId: String): Flow<SnapDetail?>

    @Query("SELECT * FROM snap_details WHERE strftime('%Y%m%d', dateTime) = :date ORDER BY dateTime DESC")
    fun getSnapsByDate(date: String): Flow<List<SnapDetail>>

    @Query("DELETE FROM snap_details WHERE id IN (:snapIds)")
    suspend fun deleteSnaps(snapIds: List<String>)
}
