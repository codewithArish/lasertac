package com.lasertrac.app.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapLocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSnapLocation(snapLocation: SavedSnapLocationEntity)

    @Query("SELECT * FROM saved_snap_location WHERE snapId = :snapId LIMIT 1")
    fun getSnapLocationById(snapId: String): Flow<SavedSnapLocationEntity?>

    @Query("SELECT * FROM saved_snap_location ORDER BY snapId DESC")
    fun getAllSnapLocations(): Flow<List<SavedSnapLocationEntity>>

    @Query("DELETE FROM saved_snap_location WHERE snapId IN (:snapIds)")
    suspend fun deleteSnapsByIds(snapIds: List<String>)
}
